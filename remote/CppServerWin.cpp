//
// Created by khari on 6/20/2023.
//
#include <windows.h>

#include "include/cef_app.h"
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include "CefUtils.h"
#include "ServerHandler.h"

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
    std::shared_ptr<TSocket> sock =
        std::dynamic_pointer_cast<TSocket>(connInfo.transport);
    Log::debug("Incoming connection\n");
    Log::debug("\tSocketInfo: %s", sock->getSocketInfo().c_str());
    Log::debug("\tPeerHost: %s", sock->getPeerHost().c_str());
    Log::debug("\tPeerAddress: %s", sock->getPeerAddress().c_str());
    Log::debug("\tPeerPort: %d", sock->getPeerPort());
    auto * serverHandler = new ServerHandler;
    Log::debug("\tServerHandler: %p\n", serverHandler);
    return serverHandler;
  }
  void releaseHandler(ServerIf* handler) override { delete handler; }
};

int main() {
  HINSTANCE hi = GetModuleHandle (0);

  Log::init(LEVEL_TRACE);
  setThreadName("main");
  CefMainArgs main_args(hi);

  Log::debug("Starting the process...");

  const int result = CefExecuteProcess(main_args, nullptr, nullptr);
  if (result >= 0) {
    return result;
  }

  CefUtils::initializeCef();

  TThreadedServer server(std::make_shared<ServerProcessorFactory>(
                             std::make_shared<ServerCloneFactory>()),
                         std::make_shared<TServerSocket>(9090),  // port
                         std::make_shared<TBufferedTransportFactory>(),
                         std::make_shared<TBinaryProtocolFactory>());

  Log::debug("Starting the server...");
  try {
    server.serve();
  } catch (const TException& e) {
    Log::error("Exception in listening thread");
    Log::error(e.what());
  }

  Log::debug("Done, server stopped.");
  return 0;
}
