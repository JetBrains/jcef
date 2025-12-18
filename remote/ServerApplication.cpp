#include "ServerApplication.h"

#include <thread>
#include <mutex>
#include <condition_variable>

#include "include/base/cef_callback.h"
#include "include/wrapper/cef_closure_task.h"
#include "include/cef_app.h"
#include "include/cef_base.h"

#include "log/Log.h"
#include "Utils.h"
#include "ServerHandler.h"
#include "ServerHandlerContext.h"
#include "RpcExecutor.h"
#include "DebugInfo.h"

#include <sstream>
#include "browser/RemoteBrowser.h"
#include "handlers/app/RemoteAppHandler.h"

#if defined(OS_LINUX)
#include <X11/Xlib.h>
#endif

#include <regex>

using namespace apache::thrift;
using namespace thrift_codegen;

namespace {
bool TRACE_HANDLERS_LIFESPAN = false;
std::regex * TRACE_THRIFT_MESSAGES_REGEXP = nullptr;
}

class MyServerProcessor : public ServerProcessor {
 public:
  MyServerProcessor(::std::shared_ptr<ServerHandler> iface) : ServerProcessor(iface), myHandler(iface) {}

  bool process(std::shared_ptr<protocol::TProtocol> in,
               std::shared_ptr<protocol::TProtocol> out,
               void* connectionContext) override {
    std::string fname;
    protocol::TMessageType mtype;
    int32_t seqid;
    in->readMessageBegin(fname, mtype, seqid);

    if (mtype != protocol::T_CALL && mtype != protocol::T_ONEWAY) {
      Log::error("received invalid message type %d from client", mtype);
      return false;
    }

    myIsProcessing = true;
    myStartDispatch = Clock::now();
    myFuncName = fname;

    if (TRACE_THRIFT_MESSAGES_REGEXP != nullptr) {
      std::smatch m;
      if(std::regex_match(fname, m, *TRACE_THRIFT_MESSAGES_REGEXP))
        Log::trace("\t process %s", fname.c_str());
    }

    const bool dispatchResult = dispatchCall(in.get(), out.get(), fname, seqid, connectionContext);
    myIsProcessing = false;
    return dispatchResult;
  }

  const std::shared_ptr<ServerHandler> getServerHandler() const { return myHandler; }
  bool isProcessing() const { return myIsProcessing; }
  Clock::time_point getStartDispatch() const { return myStartDispatch; }
  std::string getFuncName() const { return myFuncName; }

 private:
  const std::shared_ptr<ServerHandler> myHandler;

  volatile bool myIsProcessing = false;
  Clock::time_point myStartDispatch;
  std::string myFuncName;
};

class MyServerProcessorFactory : public ::apache::thrift::TProcessorFactory {
 public:
  MyServerProcessorFactory() noexcept {}

  ::std::shared_ptr< ::apache::thrift::TProcessor > getProcessor(const ::apache::thrift::TConnectionInfo& connInfo) override {
    ServerHandler * ptrServerHandler = new ServerHandler;
    if (TRACE_HANDLERS_LIFESPAN)
      Log::trace("Created ServerHandler: %p", ptrServerHandler);
    const int cooldownMs =
        ServerApplication::instance().getCmdArgs().getOpenTransportCooldownMs();
    if (cooldownMs > 0) {
      // system 'cooldown' (otherwise pipe transport may not open on Windows)
      std::this_thread::sleep_for(std::chrono::milliseconds(cooldownMs));
    }

    std::shared_ptr<ServerHandler> handler(ptrServerHandler);
    std::shared_ptr<MyServerProcessor> processor(new MyServerProcessor(handler), [&](MyServerProcessor* p){
      if (TRACE_HANDLERS_LIFESPAN)
        Log::trace("Release ServerHandler: %p", p->getServerHandler().get());
      const bool isMaster = p->getServerHandler()->isMaster();
      bool needStartShuttingDown = false;
      {
        Lock lock(myMutex);
        myProcessors.erase(p);
        if (isMaster) {
          needStartShuttingDown = true;
          for (auto processor: myProcessors)
            if (processor->getServerHandler()->isMaster()) {
              needStartShuttingDown = false;
              break;
            }
        }
      }

      delete p;

      if (needStartShuttingDown)
        ServerApplication::instance().startShuttingDown();
    });

    Lock lock(myMutex);
    myProcessors.insert(processor.get());
    return processor;
  }

  void forEach(std::function<void(const MyServerProcessor*)> visitor);

  bool hasMaster();
  std::shared_ptr<ServerHandlerContext> findCtx(int connectionId);

 protected:
  std::recursive_mutex myMutex;
  std::set<const MyServerProcessor*> myProcessors;
};

std::shared_ptr<ServerHandlerContext> MyServerProcessorFactory::findCtx(int connectionId) {
  Lock lock(myMutex);
  for (auto p: myProcessors)
    if (p->getServerHandler()->getCid() == connectionId)
      return p->getServerHandler()->getCtx();
  return nullptr;
}

void MyServerProcessorFactory::forEach(std::function<void(const MyServerProcessor*)> visitor) {
  Lock lock(myMutex);
  for (const auto& h: myProcessors)
    visitor(h);
}

struct cancelled_error {};

class CancellationPoint {
 public:
  CancellationPoint() : myStop(false) {}

  void cancel() {
    std::unique_lock<std::mutex> lock(myMutex);
    myStop = true;
    myCond.notify_all();
  }

  template <typename P>
  void wait(const P& period) {
    std::unique_lock<std::mutex> lock(myMutex);
    if (myStop || myCond.wait_for(lock, period) == std::cv_status::no_timeout)
      throw cancelled_error();
  }

 private:
  bool myStop;
  std::mutex myMutex;
  std::condition_variable myCond;
};

ServerApplication ServerApplication::ourInstance;

ServerApplication::ServerApplication() {}

ServerApplication::~ServerApplication() {
  if (myAppHandler != nullptr)
    myAppHandler->Release();
}

#if defined(OS_MAC)
namespace CefUtils {
  bool loadCefFramework();
}
#endif

void ServerApplication::onBeforeExit() {
  myStopWatcher->cancel();
}

bool ServerApplication::init(int argc, char* argv[]) {
  myTimeStart = Clock::now();
  if (!myCmdArgs.init(argc, argv)) {
    Log::debug("Show help and exit.");
    return false;
  }
  Log::init(myCmdArgs.getLogLevel(), myCmdArgs.getLogFile());
  Log::info("Init ServerApplication with transport: %s.", myCmdArgs.getTransportDesc().c_str());

  myFactory = std::make_shared<MyServerProcessorFactory>();

#if defined(OS_MAC)
  const Clock::time_point t0 = Clock::now();
  if (!CefUtils::loadCefFramework()) {
    Log::error("Can't load CEF framework library.");
    return false;
  }

  const Clock::time_point t1 = Clock::now();
  if (Log::isDebugEnabled()) {
    auto d1 = std::chrono::duration_cast<std::chrono::milliseconds>(t1 - t0);
    Log::debug("Loaded CEF framework library, spent %d ms", (int)d1.count());
  }
#elif defined(OS_LINUX)
  XInitThreads();
#endif

  CefSettings settings;
  myCmdArgs.prepareCefSettings(settings);
  myAppHandler = new RemoteAppHandler(myCmdArgs.myChromiumSwitches, settings, myCmdArgs.myCustomSchemes);
  myAppHandler->AddRef();

  // Read constants from env
  TRACE_HANDLERS_LIFESPAN = getBoolEnv("CEF_SERVER_TRACE_HANDLERS_LIFESPAN");
  const char* envTraceMessagesRegexp = getenv("CEF_SERVER_TRACE_THRIFT_MESSAGES_REGEXP");
  if (envTraceMessagesRegexp != nullptr)
    TRACE_THRIFT_MESSAGES_REGEXP = new std::regex(envTraceMessagesRegexp);

  // Init watcher thread
  myStopWatcher = std::make_shared<CancellationPoint>();
  myThreadWatcher = std::thread([&]() {
    Log::setThreadName("Watcher");
    const std::chrono::milliseconds timeoutWatchMs(getLongEnv("CEF_SERVER_timeoutWatchMs", 5000));
    std::chrono::milliseconds timeoutDebugLogMs(getLongEnv("CEF_SERVER_TimeoutStacktraceLogMs", 30 * 1000));
    std::chrono::milliseconds timeoutExecutionMs(getLongEnv("CEF_SERVER_ourTimeoutExecutionMs", 5 * 1000));
    std::chrono::milliseconds timeoutShuttingDownMs(getLongEnv("CEF_SERVER_ourTimeoutShuttingDownMs", 25 * 1000));

    Clock::time_point lastDebugLog = Clock::now() - timeoutDebugLogMs;

    while (true) {
      try {
        myStopWatcher->wait(std::chrono::milliseconds(timeoutWatchMs));
      } catch (const cancelled_error&) {
        Log::debug("Watcher thread was stopped.");
        return 0;
      }

      std::this_thread::sleep_for(timeoutWatchMs);
      const std::chrono::time_point now(Clock::now());
      myFactory->forEach([&](const MyServerProcessor* p){
        if (!p || !p->getServerHandler()) {
          Log::error("Can't be: !p || !p->getServerHandler()");
          return;
        }
        if (!p->getServerHandler()->getCtx())
          return;
        enum {
          ServerHandler,
          JavaService,
          JavaServiceIO
        };

        using namespace std::chrono_literals;
        std::chrono::duration<float, std::micro> execTimes[] = {0us, 0us, 0us};

        const std::chrono::time_point now(Clock::now());
        // 1. Check ServerHandler timings
        if (p->isProcessing())
          execTimes[ServerHandler] = now - p->getStartDispatch();

        // 2. Check JavaService timings
        std::shared_ptr<RpcExecutor> rpcExecutor = p->getServerHandler()->getCtx()->javaService();
        if (rpcExecutor && rpcExecutor->isProcessing())
          execTimes[JavaService] = now - rpcExecutor->getProcessingStart();
          //printDebugIfNecessary(,);

        std::shared_ptr<RpcExecutor> rpcExecutorIO = p->getServerHandler()->getCtx()->javaServiceIO();
        if (rpcExecutorIO && rpcExecutorIO->isProcessing())
          execTimes[JavaServiceIO] = now - rpcExecutorIO->getProcessingStart();

          //printDebugIfNecessary(rpcExecutorIO->getProcessingName(),
        const bool isTimeoutServerHandler = execTimes[ServerHandler] > timeoutExecutionMs;
        const bool isTimeoutJavaService = execTimes[JavaService] > timeoutExecutionMs;
        const bool isTimeoutJavaServiceIO = execTimes[JavaServiceIO] > timeoutExecutionMs;
        const bool needPrintDebugLog = (now - lastDebugLog) >= timeoutDebugLogMs;
        if (isTimeoutServerHandler || isTimeoutJavaService || isTimeoutJavaServiceIO) {
          if (needPrintDebugLog) {
            Log::warn("Execution time exceeds timeout. Probably deadlock occurred. Events:");
            if (isTimeoutServerHandler)
              Log::info("Java2Native function '%s' executes more than %d seconds",
                        p->getFuncName().c_str(), (int)(std::chrono::duration_cast<std::chrono::seconds>(execTimes[ServerHandler])).count());
            else if (p->isProcessing())
              Log::info("Java2Native function '%s' was started.", p->getFuncName().c_str());
            else
              Log::info("Java2Native function '%s' was finished.", p->getFuncName().c_str());

            if (isTimeoutJavaService)
              Log::info("Native2Java function '%s' executes more than %d seconds",
                        rpcExecutor->getProcessingName().c_str(), (int)(std::chrono::duration_cast<std::chrono::seconds>(execTimes[JavaService])).count());
            else if (rpcExecutor->isProcessing())
              Log::info("Native2Java function '%s' was started.", rpcExecutor->getProcessingName().c_str());
            else
              Log::info("Native2Java function '%s' was finished.", rpcExecutor->getProcessingName().c_str());

            if (isTimeoutJavaServiceIO)
              Log::info("Native2Java IO function '%s' executes more than %d seconds",
                        rpcExecutorIO->getProcessingName().c_str(), (int)(std::chrono::duration_cast<std::chrono::seconds>(execTimes[JavaServiceIO])).count());
            else if (rpcExecutorIO->isProcessing())
              Log::info("Native2Java IO function '%s' was started.", rpcExecutorIO->getProcessingName().c_str());
            else
              Log::info("Native2Java IO function '%s' was finished.", rpcExecutorIO->getProcessingName().c_str());
          }
        }
      });

      // 3. Check application timings
      bool needShutdownHard = false;
      {
        Lock lock(myMutexState);
        if (myState >= SS_SHUTTING_DOWN) {
          std::chrono::duration<float, std::micro> elapsed = now - myTimeStartShuttingDown;
          if (elapsed > timeoutShuttingDownMs) {
            Log::warn("Start hard shutdown (elapsed %d ms)", std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count());
            myState = SS_SHUTDOWN;
            needShutdownHard = true;
          }
        }
      }
      if (needShutdownHard) {
        CefPostTask(TID_UI, base::BindOnce(CefQuitMessageLoop));
        Log::debug("CefQuitMessageLoop is posted (to be executed on UI thread), wait a little before exit...");
        std::this_thread::sleep_for(std::chrono::milliseconds(2000));
        Log::debug("Buy [%s]!", myCmdArgs.getTransportDesc().c_str());
        std::exit(0);
      }
    }
  });
  myThreadWatcher.detach();
  return true;
}

std::shared_ptr<apache::thrift::TProcessorFactory> ServerApplication::getProcessorFactory() const {
  return myFactory;
}

void ServerApplication::startShuttingDown() {
  {
    Lock lock(myMutexState);
    if (myState >= SS_SHUTTING_DOWN)
      return;

    myState = SS_SHUTTING_DOWN;
    myTimeStartShuttingDown = Clock::now();
  }

  myThreadShutdown = std::thread([&]() {
    Log::setThreadName("Shutdown");
    Clock::time_point start = Clock::now();
    const std::chrono::milliseconds timeout(getLongEnv("CEF_SERVER_timeoutShutdownMs", 15000));

    while (Clock::now() - start < timeout) {
      std::this_thread::sleep_for(std::chrono::milliseconds(1000));
      const std::chrono::time_point now(Clock::now());

      if (RemoteBrowser::getAllBrowsersCount() == 0) {
        {
          Lock lock(myMutexState);
          myState = SS_SHUTDOWN;
        }
        CefPostTask(TID_UI, base::BindOnce(CefQuitMessageLoop));
        Log::debug("CefQuitMessageLoop will be invoked now (on TID_UI).");
        return;
      }

      if (Log::isTraceEnabled()) {
        std::vector<int> remainingBids = RemoteBrowser::enumAllBrowsers();
        std::stringstream ss;
        for (int bid : remainingBids)
          ss << bid << ", ";
        Log::trace("Server is waiting for closing remaining browsers with bids: %s", ss.str().c_str());
      }
    }
    Log::trace("Exit shutdown-loop because of timeout.");
    CefPostTask(TID_UI, base::BindOnce(CefQuitMessageLoop)); // not sure that it is necessary here..
  });
  myThreadShutdown.detach();
}

std::string ServerApplication::getRootPath() const {
  return myAppHandler->getRootPath();
}

bool ServerApplication::isDefaultRoot() const {
  return myAppHandler->isDefaultRoot();
}

const std::chrono::high_resolution_clock::time_point& ServerApplication::getStartTime() const {
  return myTimeStart;
}

std::string ServerApplication::getState() {
  Lock lock(myMutexState);
  switch (myState) {
    case SS_NEW: return "SS_NEW";
    case SS_SHUTDOWN: return "SHUTDOWN";
    case SS_SHUTTING_DOWN: return "SHUTTING_DOWN";
    default: return "UNKNOWN_STATE";
  }
}

std::string ServerApplication::getStateWithDetails() {
  std::stringstream ss;

  ss << "Server state: " << getState() << std::endl;

  ss << "Server handlers:" << std::endl;
  myFactory->forEach([&](const MyServerProcessor* p) {
    if (!p || !p->getServerHandler()) return; // can't be
    ss << p->getServerHandler()->getDebugInfo(1);
  });
  ss << std::endl;

  ss << "RemoteObject factories:" << std::endl;
  ss << DebugInfo::getInfo(1);

  return ss.str();
}

std::shared_ptr<ServerHandlerContext> ServerApplication::getCtx(int connectionId) {
    return myFactory->findCtx(connectionId);
}
