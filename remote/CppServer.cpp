#include <thrift/concurrency/ThreadFactory.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include <iostream>

#include "CefUtils.h"
#include "ServerHandler.h"
#include "log/Log.h"

#include "include/cef_app.h"

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::server;

/*
  ServerIfFactory is code generated.
  ServerCloneFactory is useful for getting access to the server side of the
  transport.  It is also useful for making per-connection state.  Without this
  CloneFactory, all connections will end up sharing the same handler instance.
*/
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
    ServerHandler * serverHandler = new ServerHandler;
    Log::debug("\tServerHandler: %p\n", serverHandler);
    return serverHandler;
  }
  void releaseHandler(ServerIf* handler) override { delete handler; }
};

int main(int argc, char* argv[]) {
  Log::init();
  preinitCef(argc, argv);

  std::thread serverThread([=]() {
    TThreadedServer server(std::make_shared<ServerProcessorFactory>(
                               std::make_shared<ServerCloneFactory>()),
                           std::make_shared<TServerSocket>(9090),  // port
                           std::make_shared<TBufferedTransportFactory>(),
                           std::make_shared<TBinaryProtocolFactory>());

    /*
    // if you don't need per-connection state, do the following instead
    TThreadedServer server(
      std::make_shared<ServerProcessor>(std::make_shared<ServerHandler>()),
      std::make_shared<TServerSocket>(9090), //port
      std::make_shared<TBufferedTransportFactory>(),
      std::make_shared<TBinaryProtocolFactory>());
    */

    /**
     * Here are some alternate server types...

    // This server only allows one connection at a time, but spawns no threads
    TSimpleServer server(
      std::make_shared<ServerProcessor>(std::make_shared<ServerHandler>()),
      std::make_shared<TServerSocket>(9090),
      std::make_shared<TBufferedTransportFactory>(),
      std::make_shared<TBinaryProtocolFactory>());

    const int workerCount = 4;

    std::shared_ptr<ThreadManager> threadManager =
      ThreadManager::newSimpleThreadManager(workerCount);
    threadManager->threadFactory(
      std::make_shared<ThreadFactory>());
    threadManager->start();

    // This server allows "workerCount" connection at a time, and reuses threads
    TThreadPoolServer server(
      std::make_shared<ServerProcessorFactory>(std::make_shared<ServerCloneFactory>()),
      std::make_shared<TServerSocket>(9090),
      std::make_shared<TBufferedTransportFactory>(),
      std::make_shared<TBinaryProtocolFactory>(),
      threadManager);
    */
    Log::debug("starting the server...");
    server.serve();
    Log::debug("done, server stopped.");
  });

  CefRunMessageLoop();
  CefShutdown();

  return 0;
}
