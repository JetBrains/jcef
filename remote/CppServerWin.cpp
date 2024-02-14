//
// Created by khari on 6/20/2023.
//
#include <windows.h>

#include "include/cef_app.h"
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TTransportUtils.h>
#include <thrift/transport/TServerSocket.h>

#include "CefUtils.h"
#include "ServerHandler.h"
#include "windows/PipeTransportServer.h"
#include "handlers/app/HelperApp.h"

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::server;

using namespace thrift_codegen;

class ServerCloneFactory : virtual public ServerIfFactory {
 public:
  ~ServerCloneFactory() override = default;
  ServerIf* getHandler(
      const ::apache::thrift::TConnectionInfo& connInfo) override {
    auto * serverHandler = new ServerHandler;
    Log::debug("\tCreated new ServerHandler: %p\n", serverHandler);
    return serverHandler;
  }
  void releaseHandler(ServerIf* handler) override { delete handler; }
};

int main(int argc, char* argv[]) {
  HINSTANCE hi = GetModuleHandle (0);

  CommandLineArgs cmdArgs(argc, argv);
  Log::init(LEVEL_TRACE, cmdArgs.getLogFile());
  setThreadName("main");
  CefMainArgs main_args(hi);

  CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();
  command_line->InitFromString(::GetCommandLineW());

  CefRefPtr<CefApp> app = nullptr;
  const std::string& process_type = command_line->GetSwitchValue("type");
  if (process_type == "renderer")
    app = new HelperApp();

  const int result = CefExecuteProcess(main_args, app, nullptr);
  if (result >= 0) {
    return result;
  }

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
    Log::info("Pipe transport will be used, path=%s", cmdArgs.getPipe().c_str());
    serverTransport = std::make_shared<PipeTransportServer>("\\\\.\\pipe\\cef_server_pipe");
  }

  std::shared_ptr<TThreadedServer> server = std::make_shared<TThreadedServer>(
      std::make_shared<ServerProcessorFactory>(
      std::make_shared<ServerCloneFactory>()),
      serverTransport,
      std::make_shared<TBufferedTransportFactory>(),
      std::make_shared<TBinaryProtocolFactory>());

  std::thread servThread([=]() {
    Log::debug("Starting the server...");

    try {
      server->serve();
    } catch (TException e) {
      Log::error("Exception in listening thread");
      Log::error(e.what());
    }

    Log::debug("Done, server stopped.");
  });

  CefUtils::runCefLoop();
  server->stop();
  return 0;
}
