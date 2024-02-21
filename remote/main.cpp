#ifdef WIN32
#include <windows.h>
#include "windows/PipeTransportServer.h"
#include "include/cef_app.h"
#endif //WIN32

#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include "CefUtils.h"
#include "ServerHandler.h"
#include "log/Log.h"

#include "handlers/app/HelperApp.h"

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::server;

using namespace thrift_codegen;

/*
  ServerIfFactory is code generated.
  ServerCloneFactory is useful for getting access to the server side of the
  transport.  It is also useful for making per-connection state.  Without this
  CloneFactory, all connections will end up sharing the same handler instance.
*/
class ServerCloneFactory : virtual public ServerIfFactory {
 public:
  ~ServerCloneFactory() override = default;
  ServerIf* getHandler(const ::apache::thrift::TConnectionInfo& connInfo) override {
    std::shared_ptr<TSocket> sock = std::dynamic_pointer_cast<TSocket>(connInfo.transport);
    ServerHandler * serverHandler = new ServerHandler;
    Log::trace("Created new ServerHandler: %p", serverHandler);
    return serverHandler;
  }
  void releaseHandler(ServerIf* handler) override {
    Log::trace("Release ServerHandler: %p", handler);
    delete handler;
  }
};

#ifdef OS_MAC
extern void initMacApplication();
#endif

class MyServerProcessor : public ServerProcessor {
 public:
  MyServerProcessor(::std::shared_ptr<ServerIf> iface) : ServerProcessor(iface) {}

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

    //Log::trace("\t process %s", fname.c_str());
    return dispatchCall(in.get(), out.get(), fname, seqid, connectionContext);
  }
};

class MyServerProcessorFactory : public ::apache::thrift::TProcessorFactory {
 public:
  MyServerProcessorFactory(const ::std::shared_ptr< ServerIfFactory >& handlerFactory) noexcept :
        handlerFactory_(handlerFactory) {}


  ::std::shared_ptr< ::apache::thrift::TProcessor > getProcessor(const ::apache::thrift::TConnectionInfo& connInfo) override {
    ::apache::thrift::ReleaseHandler< ServerIfFactory > cleanup(handlerFactory_);
    ::std::shared_ptr< ServerIf > handler(handlerFactory_->getHandler(connInfo), cleanup);
    ::std::shared_ptr< ::apache::thrift::TProcessor > processor(new MyServerProcessor(handler));
    return processor;
  }

 protected:
  ::std::shared_ptr< ServerIfFactory > handlerFactory_;
};

int main(int argc, char* argv[]) {
  CommandLineArgs cmdArgs(argc, argv);
  Log::init(LEVEL_TRACE, cmdArgs.getLogFile());

  setThreadName("main");
#if defined(OS_LINUX)
  CefRefPtr<CefApp> app = nullptr;
  CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();
  command_line->InitFromArgv(argc, argv);
  const std::string& process_type = command_line->GetSwitchValue("type");
  if (process_type == "renderer" || process_type == "zygote")
    app = new HelperApp();
  // On Linux the zygote process is used to spawn other process types. Since
  // we don't know what type of process it will be give it the renderer
  // client.

  CefMainArgs main_args(argc, argv);
  int exit_code = CefExecuteProcess(main_args, app, nullptr);
  if (exit_code >= 0) {
    return exit_code;
  }
#elif WIN32
  CefRefPtr<CefApp> app = nullptr;
  CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();
  command_line->InitFromString(::GetCommandLineW());
  const std::string& process_type = command_line->GetSwitchValue("type");
  if (process_type == "renderer")
    app = new HelperApp();

  CefMainArgs main_args(GetModuleHandle(0));
  const int result = CefExecuteProcess(main_args, app, nullptr);
  if (result >= 0) {
    return result;
  }
#elif OS_MAC
  initMacApplication();
#endif
  const Clock::time_point startTime = Clock::now();

  const bool success = CefUtils::initializeCef(cmdArgs.getParamsFile());
  if (!success) {
    Log::error("Cef initialization failed");
    return -2;
  }

  std::shared_ptr<TServerTransport> serverTransport;
  if (cmdArgs.useTcp()) {
    Log::info("TCP transport will be used, port=%d", cmdArgs.getPort());
    serverTransport = std::make_shared<TServerSocket>(cmdArgs.getPort());
  } else {
    std::string pipePath = cmdArgs.getPipe();
    if (pipePath.empty()) {
      Log::error("Pipe path is empty, exit.");
      return -3;
    }
#ifdef WIN32
    if (pipePath.rfind("\\\\.\\pipe\\", 0) != 0)
      pipePath = "\\\\.\\pipe\\" + pipePath;
    Log::info("Windows-pipe transport will be used, path=%s", pipePath.c_str());
    serverTransport = std::make_shared<PipeTransportServer>(pipePath);
#else
    Log::info("Pipe transport will be used, path=%s", pipePath.c_str());
    std::remove(pipePath.c_str());
    serverTransport = std::make_shared<TServerSocket>(pipePath.c_str());
#endif //WIN32
  }
  std::shared_ptr<TThreadedServer> server = std::make_shared<TThreadedServer>(
      std::make_shared<MyServerProcessorFactory>(
      std::make_shared<ServerCloneFactory>()),
      serverTransport,
      std::make_shared<TBufferedTransportFactory>(),
      std::make_shared<TBinaryProtocolFactory>());

  if (Log::isDebugEnabled()) {
    const Clock::time_point endTime = Clock::now();
    Duration d2 = std::chrono::duration_cast<std::chrono::microseconds>(endTime - startTime);
    Log::debug("Starting the server. Initialization spent %d ms", (int)d2.count()/1000);
  }

  std::thread servThread([=]() {
    setThreadName("ServerListener");
    try {
      Log::debug("Start listening incoming connections."); // TODO: remove
      server->serve();
    } catch (TException& e) {
      Log::error("Exception in listening thread");
      Log::error(e.what());
    } catch (...) {
      Log::error("Unknown exception in listening thread");
    }
    Log::debug("Done, server stopped.");
  });

  std::thread testThread;
  if (cmdArgs.isTestMode()) {
    const int timeoutSec = 30;
    Log::info("Server will be started in test mode, exit timeout = %d sec.", timeoutSec);
    testThread = std::thread([&]() {
      setThreadName("TestMonitor");
      std::chrono::time_point startTime(Clock::now());
      std::chrono::duration<float, std::milli> elapsed;
      std::chrono::duration<float, std::milli> timeout(timeoutSec*1000);
      while ((elapsed = (Clock::now() - startTime)) < timeout) {
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
        const int remainMs = (timeout - elapsed).count();
        Log::debug("\t will exit in %d sec...", remainMs);
      }

      Log::info("Timeout elapsed, do exit.");
      ServerHandler::setStateShutdown();
      std::this_thread::sleep_for(std::chrono::milliseconds(3000));
      Log::info("Buy!");
      std::exit(0);
    });
  }

  CefUtils::runCefLoop();
  Log::debug("Finished message loop.");
  server->stop();
  servThread.join();
  Log::debug("Buy!");
  return 0;
}
