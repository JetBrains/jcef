#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include "CefUtils.h"
#include "ServerHandler.h"
#include "log/Log.h"

#include "include/cef_app.h"

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
  Log::init(LEVEL_TRACE);
  setThreadName("main");
#if defined(OS_LINUX)
  CefMainArgs main_args(argc, argv);
  CefRefPtr<CefApp> app = nullptr;
  CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();
  command_line->InitFromArgv(argc, argv);
  const std::string& process_type = command_line->GetSwitchValue("type");
  if (process_type == "renderer")
    app = new HelperApp();

  int exit_code = CefExecuteProcess(main_args, app, nullptr);
  if (exit_code >= 0) {
    return exit_code;
  }
#elif OS_MAC
  initMacApplication();
#endif
  const Clock::time_point startTime = Clock::now();

  const bool success = CefUtils::initializeCef(argc, argv);
  if (!success) {
    Log::error("Cef initialization failed");
    return -2;
  }

  boost::filesystem::path pipePath = boost::filesystem::temp_directory_path().append("cef_server_pipe").lexically_normal();
  std::remove(pipePath.c_str());
  std::shared_ptr<TThreadedServer> server = std::make_shared<TThreadedServer>(
      std::make_shared<ServerProcessorFactory>(
      std::make_shared<ServerCloneFactory>()),
      std::make_shared<TServerSocket>(pipePath.c_str()),
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
      server->serve();
    } catch (TException e) {
      Log::error("Exception in listening thread");
      Log::error(e.what());
    }

    Log::debug("Done, server stopped.");
  });

  CefUtils::runCefLoop();
  server->stop();
  servThread.join();
  return 0;
}
