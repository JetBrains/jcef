#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include "CefUtils.h"
#include "ServerHandler.h"
#include "log/Log.h"

#include "handlers/app/HelperApp.h"

#include <boost/filesystem.hpp>

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
    Log::trace("Created new ServerHandler: %p\n", serverHandler);
    return serverHandler;
  }
  void releaseHandler(ServerIf* handler) override {
    Log::trace("Release ServerHandler: %p\n", handler);
    delete handler;
  }
};

#ifdef OS_MAC
extern void initMacApplication();
#endif

int main(int argc, char* argv[]) {
  CommandLineArgs cmdArgs(argc, argv);
  Log::init(LEVEL_TRACE, cmdArgs.getLogFile());

  setThreadName("main");
#if defined(OS_LINUX)
  CefMainArgs main_args(argc, argv);
  CefRefPtr<CefApp> app = nullptr;
  CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();
  command_line->InitFromArgv(argc, argv);
  const std::string& process_type = command_line->GetSwitchValue("type");
  if (process_type == "renderer" || process_type == "zygote")
    app = new HelperApp();
  // On Linux the zygote process is used to spawn other process types. Since
  // we don't know what type of process it will be give it the renderer
  // client.

  int exit_code = CefExecuteProcess(main_args, app, nullptr);
  if (exit_code >= 0) {
    return exit_code;
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
    const std::string pipePath = cmdArgs.getPipe();
    if (pipePath.empty()) {
      Log::error("Pipe path is empty, exit.");
      return -3;
    }
    std::remove(pipePath.c_str());
    Log::info("Pipe transport will be used, path=%s", cmdArgs.getPipe().c_str());
    serverTransport = std::make_shared<TServerSocket>(cmdArgs.getPipe().c_str());
  }
  std::shared_ptr<TThreadedServer> server = std::make_shared<TThreadedServer>(
      std::make_shared<ServerProcessorFactory>(
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
    } catch (TException e) {
      Log::error("Exception in listening thread");
      Log::error(e.what());
    }
    Log::debug("Done, server stopped.");
  });

  CefUtils::runCefLoop();
  Log::debug("Finished message loop.");
  server->stop();
  servThread.join();
  Log::debug("Buy!");
  return 0;
}
