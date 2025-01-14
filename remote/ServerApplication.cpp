#include "ServerApplication.h"

#include <thread>

#include "include/base/cef_callback.h"
#include "include/wrapper/cef_closure_task.h"
#include "include/cef_app.h"

#include "log/Log.h"
#include "Utils.h"
#include "ServerHandler.h"
#include "ServerHandlerContext.h"
#include "browser/ClientsManager.h"
#include "RpcExecutor.h"

#include <sstream>
#include "CefSettingsParser.h"
#include "handlers/app/RemoteAppHandler.h"

#if defined(OS_LINUX)
#include <X11/Xlib.h>
#endif

using namespace apache::thrift;
using namespace thrift_codegen;

namespace {
bool TRACE_HANDLERS_LIFESPAN = false;
bool TRACE_THRIFT_MESSAGES = false;

std::chrono::milliseconds ourTimeoutDebugLogMs(30 * 1000);
std::chrono::milliseconds ourTimeoutExecutionMs(5 * 1000);
std::chrono::milliseconds ourTimeoutShuttingDownMs(10 * 1000);
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

    if (TRACE_THRIFT_MESSAGES)
      Log::trace("\t process %s", fname.c_str());

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
      {
        Lock lock(myMutex);
        myProcessors.erase(p);
      }
      delete p;
    });

    Lock lock(myMutex);
    myProcessors.insert(processor.get());
    return processor;
  }

  void forEach(std::function<void(const MyServerProcessor*)> visitor);

  bool hasMaster();
  std::shared_ptr<ServerHandlerContext> findCtx(int cid);

 protected:
  std::recursive_mutex myMutex;
  std::set<const MyServerProcessor*> myProcessors;
};

bool MyServerProcessorFactory::hasMaster() {
  Lock lock(myMutex);
  for (auto p: myProcessors)
    if (p->getServerHandler()->isMaster() && !p->getServerHandler()->isClosed())
      return true;
  return false;
}

std::shared_ptr<ServerHandlerContext> MyServerProcessorFactory::findCtx(int cid) {
  Lock lock(myMutex);
  for (auto p: myProcessors)
    if (p->getServerHandler()->getCid() == cid)
      return p->getServerHandler()->getCtx();
  return nullptr;
}

void MyServerProcessorFactory::forEach(std::function<void(const MyServerProcessor*)> visitor) {
  Lock lock(myMutex);
  for (const auto& h: myProcessors)
    visitor(h);
}

ServerApplication ServerApplication::ourInstance;

ServerApplication::ServerApplication() : myFactory(new MyServerProcessorFactory) {}

ServerApplication::~ServerApplication() {
  myAppHandler->Release();
}

#if defined(OS_MAC)
namespace CefUtils {
  bool loadCefFramework();
}
#endif

void ServerApplication::init(int argc, char* argv[]) {
  myStartTime = Clock::now();
  myCmdArgs.init(argc, argv);
  Log::init(myCmdArgs.getLogLevel(), myCmdArgs.getLogFile());
  Log::info("Init ServerApplication with transport %s.\n", myCmdArgs.getTransportDesc().c_str());

#if defined(OS_MAC)
  const Clock::time_point t0 = Clock::now();
  if (!CefUtils::loadCefFramework()) {
    Log::error("Can't load CEF framework library.");
    return;
  }

  const Clock::time_point t1 = Clock::now();
  if (Log::isDebugEnabled()) {
    Duration d1 = std::chrono::duration_cast<std::chrono::milliseconds>(t1 - t0);
    Log::debug("Loaded CEF framework library, spent %d ms", (int)d1.count());
  }
#elif defined(OS_LINUX)
  XInitThreads();
#endif

  std::vector<std::string> cmdlineSwitches;
  CefSettings settings;
  std::vector<std::pair<std::string, int>> schemes;
  CefSettingsParser::parseSettings(myCmdArgs.getParamsFile(), cmdlineSwitches, settings, schemes);

  myAppHandler = new RemoteAppHandler(cmdlineSwitches, settings, schemes);
  myAppHandler->AddRef();

  // Read constants from env
  TRACE_HANDLERS_LIFESPAN = getBoolEnv("CEF_SERVER_TRACE_HANDLERS_LIFESPAN");
  TRACE_THRIFT_MESSAGES = getBoolEnv("CEF_SERVER_TRACE_MESSAGES");
  ourTimeoutDebugLogMs = std::chrono::milliseconds(getLongEnv("CEF_SERVER_TimeoutStacktraceLogMs", ourTimeoutDebugLogMs.count()));
  ourTimeoutExecutionMs = std::chrono::milliseconds(getLongEnv("CEF_SERVER_ourTimeoutExecutionMs", ourTimeoutExecutionMs.count()));
  ourTimeoutShuttingDownMs = std::chrono::milliseconds(getLongEnv("CEF_SERVER_ourTimeoutShuttingDownMs", ourTimeoutShuttingDownMs.count()));

  // Init watcher thread
  myThreadWatcher = std::thread([&]() {
    setThreadName("Watcher");
    const std::chrono::milliseconds timeoutWatchMs(getLongEnv("CEF_SERVER_timeoutWatchMs", 5000));

    Clock::time_point lastDebugLog = Clock::now() - ourTimeoutDebugLogMs;

    while (!myStopWatcher) {
      std::this_thread::sleep_for(timeoutWatchMs);
      const std::chrono::time_point now(Clock::now());
      myFactory->forEach([&](const MyServerProcessor* p){
        enum {
          ServerHandler,
          JavaService,
          JavaServiceIO
        };
        Duration execTimes[] = {
            std::chrono::milliseconds(0),
            std::chrono::milliseconds(0),
            std::chrono::milliseconds(0)
        };

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
        const bool isTimeoutServerHandler = execTimes[ServerHandler] > ourTimeoutExecutionMs;
        const bool isTimeoutJavaService = execTimes[JavaService] > ourTimeoutExecutionMs;
        const bool isTimeoutJavaServiceIO = execTimes[JavaServiceIO] > ourTimeoutExecutionMs;
        const bool needPrintDebugLog = (now - lastDebugLog) >= ourTimeoutDebugLogMs;
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
      if (myState >= SS_SHUTTING_DOWN) {
        Duration elapsed;
        {
          Lock lock(myMutex);
          elapsed = now - myLastStateChange;
        }
        if (elapsed > ourTimeoutShuttingDownMs) {
          Log::warn("Start hard shutdown (state=%d, elapsed %d ms)", myState, std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count());
          shutdownHard();
        }
      }
    }
  });
  myThreadWatcher.detach();
}

std::shared_ptr<apache::thrift::TProcessorFactory> ServerApplication::getProcessorFactory() const {
  return myFactory;
}

void ServerApplication::setState(State state, std::string desc) {
  Lock lock(myMutex);
  if (myState < state) {
    myState = state;
    myLastStateChange = Clock::now();
    myStateDesc = desc;
  }
}

// Called from ServerHandler::stop
// Thread: ServerHandler-executor
void ServerApplication::startShuttingDown() {
  setState(SS_SHUTTING_DOWN, "shutting down by user request");
}

std::string ServerApplication::getStateDesc() const {
  return myStateDesc + myRemainingBrowsersDesc;
}

bool ServerApplication::isShuttingDown() {
  Lock lock(myMutex);
  return myState == SS_SHUTTING_DOWN;
}

void ServerApplication::processShuttingDownIfNecessary() {
  if (!isShuttingDown())
    return;

  std::vector<int> remainingBids;
  myFactory->forEach([&](const MyServerProcessor* p){
    std::vector<int> bids = p->getServerHandler()->getCtx()->clientsManager()->enumAllBrowsers();
    remainingBids.insert(remainingBids.end(), bids.begin(), bids.end());
  });

  if (remainingBids.empty()) {
    setState(SS_SHUTDOWN, "shutdown (quit cef msg loop)");
    CefPostTask(TID_UI, base::BindOnce(CefQuitMessageLoop));
    Log::debug("CefQuitMessageLoop will be invoked now (on TID_UI).");
  } else {
    std::stringstream ss; for (int bid : remainingBids) ss << bid << ", ";
    myRemainingBrowsersDesc = " (remaining bids: " + ss.str() + ")";
  }
  Log::debug("Server state: %s", getStateDesc().c_str());
}

void ServerApplication::onClientDestroyed() {
  processShuttingDownIfNecessary();
}

void ServerApplication::onServerHandlerClosed(const ServerHandler & handler) {
  if (handler.isMaster() && !myFactory->hasMaster()) {
    setState(SS_SHUTTING_DOWN, string_format("shutting down (closed last master handler %p)", &handler));
    Log::debug("ServerHandler %p was closed and there are no master handlers now, so shutting down server [%s].", &handler, myCmdArgs.getTransportDesc().c_str());
  }

  processShuttingDownIfNecessary();
}

void ServerApplication::shutdownHard() {
  setState(SS_SHUTDOWN, "hard shutdown");
  CefPostTask(TID_UI, base::BindOnce(CefQuitMessageLoop));
  Log::debug("CefQuitMessageLoop is posted (to be executed on UI thread), wait a little before exit...");
  std::this_thread::sleep_for(std::chrono::milliseconds(1000));
  Log::debug("Buy [%s]!", myCmdArgs.getTransportDesc().c_str());
  std::exit(0);
}

std::string ServerApplication::getRootPath() const {
  return myAppHandler->getRootPath();
}

bool ServerApplication::isDefaultRoot() const {
  return myAppHandler->isDefaultRoot();
}

const std::chrono::high_resolution_clock::time_point& ServerApplication::getStartTime() const {
  return myStartTime;
}

std::shared_ptr<ServerHandlerContext> ServerApplication::getCtx(int cid) {
    return myFactory-> findCtx(cid);
}

CommandLineArgs::CommandLineArgs() {
  const long defVal = myOpenTransportCooldownMs;
  myOpenTransportCooldownMs = getLongEnv("CEF_SERVER_TRANSPORT_OPEN_COOLDOWN_MS", defVal);
  if (myOpenTransportCooldownMs != defVal) {
    if (myOpenTransportCooldownMs < 0) myOpenTransportCooldownMs = 0;
    if (myOpenTransportCooldownMs > 500) myOpenTransportCooldownMs = 500;
    fprintf(stderr, "\tUse OpenTransportCooldownMs=%d\n", myOpenTransportCooldownMs);
  }
}

void CommandLineArgs::init(int argc, char* argv[]) {
  for (int c = 0; c < argc; ++c) {
    const char * arg = argv[c];
    if (arg == nullptr)
      continue;

    // NOTE: these switches don't conflict with chromium one.
    // See https://peter.sh/experiments/chromium-command-line-switches/
    std::string str(arg);
    if (str == "--cef-server-wait-debugger") {
      myWaitDebugger = true;
      continue;
    }

    size_t tokenPos;
    if ((tokenPos = str.find("--port=")) != str.npos) {
      std::string val = str.substr(tokenPos + 7);
      myPort = std::stoi(val);
      myUseTcp = true;
    } else if ((tokenPos = str.find("--pipe=")) != str.npos) {
      myPathPipe = str.substr(tokenPos + 7);
    } else if ((tokenPos = str.find("--logfile=")) != str.npos) {
      myPathLogFile = str.substr(tokenPos + 10);
    } else if ((tokenPos = str.find("--loglevel=")) != str.npos) {
      std::string sval = str.substr(tokenPos + 11);
      myLogLevel = std::stoi(sval);
      if (myLogLevel < LEVEL_TRACE - 5) myLogLevel = LEVEL_TRACE - 5;
      if (myLogLevel > LEVEL_FATAL) myLogLevel = LEVEL_FATAL;
    } else if ((tokenPos = str.find("--params=")) != str.npos) {
      myPathParamsFile = str.substr(tokenPos + 9);
    } else if ((tokenPos = str.find("--deleteRootCacheDir")) != str.npos) {
      myDeleteRootCacheDir = true;
    }
  }
}
