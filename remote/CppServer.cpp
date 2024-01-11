#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include <boost/filesystem.hpp>

#include <cstdio>

#include "CefUtils.h"
#include "ServerHandler.h"
#include "log/Log.h"

#include "include/cef_app.h"

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

namespace {
  class ServerHandlerTest : public thrift_codegen::ServerNull {
    int32_t arg = -1;
    int32_t connect(const int32_t backwardConnectionPort, const std::vector<std::string>& cmdLineArgs, const std::map<std::string, std::string>& settings) override {
      arg = backwardConnectionPort;
      fprintf(stderr, "Connected, arg=%d\n", arg);
      return arg + 1;
    }
    ~ServerHandlerTest() override {
      fprintf(stderr, "Disconnected, arg=%d\n", arg);
    }
  };

  class TestFactory : virtual public ServerIfFactory {
   public:
    ~TestFactory() override = default;
    ServerIf* getHandler(const ::apache::thrift::TConnectionInfo& connInfo) override {
      return new ServerHandlerTest;
    }
    void releaseHandler(ServerIf* handler) override { delete handler; }
  };

  bool _testPipeNativeServer() {
    boost::filesystem::path pipePath = boost::filesystem::temp_directory_path().append("server_pipe").lexically_normal();
    std::remove(pipePath.c_str());

    // For non-blocking use: TNonblockingServerSocket * socket = new TNonblockingServerSocket(pipePath.c_str());
    // std::shared_ptr<TNonblockingServerTransport> transport(socket);
    // std::shared_ptr<ThreadManager> threadManager = ThreadManager::newSimpleThreadManager(10);
    // std::shared_ptr<ThreadFactory> threadFactory = std::shared_ptr<ThreadFactory>(new ThreadFactory());
    // threadManager->threadFactory(threadFactory);
    // threadManager->start();
    // std::shared_ptr<TProtocolFactory> protocolFactory(new TBinaryProtocolFactory());
    // std::shared_ptr<ServerHandler> handler(new ServerHandlerTest());
    // std::shared_ptr<TProcessor> processor(new ServerProcessor(handler));
    // TNonblockingServer server(processor, protocolFactory, transport, threadManager);

    TServerSocket * socket = new TServerSocket(pipePath.c_str());
    std::shared_ptr<TServerTransport> transport(socket);
    TThreadedServer server(std::make_shared<ServerProcessorFactory>(
                               std::make_shared<TestFactory>()),
                           transport,
                           std::make_shared<TBufferedTransportFactory>(),
                           std::make_shared<TBinaryProtocolFactory>());

    Log::debug("Starting test server...");
    try {
      server.serve();
    } catch (TException e) {
      Log::error("Exception in listening thread");
      Log::error(e.what());
    }
    Log::debug("Finished test.");

    return true;
  }

  bool _testPipeNativeClient() {
    // Connect to client
    boost::filesystem::path pipePath = boost::filesystem::temp_directory_path().append("client_pipe").lexically_normal();

    try {
      std::shared_ptr<TTransport> transport = std::make_shared<TSocket>(pipePath.c_str());
      // NOTE: for non-blocking server use:
      // std::shared_ptr<TTransport> transport = std::make_shared<TFramedTransport>(std::make_shared<TSocket>(pathClient));
      std::shared_ptr<ClientHandlersClient> service =
          std::make_shared<ClientHandlersClient>(std::make_shared<TBinaryProtocol>(transport));

      transport->open();
      const int32_t cid = service->connect();
      Log::debug("Test connection to java server established, cid=%d", cid);
      service->log("This message is sent via named pipe.");
    } catch (TException& tx) {
      Log::error(tx.what());
      return -1;
    }
  }
}

int main(int argc, char* argv[]) {
  Log::init(LEVEL_TRACE);
  setThreadName("main");

  _testPipeNativeClient();
//  _testPipeNativeServer();
  if (true)
    return 0;

#if defined(OS_MAC)
  if (!CefUtils::doLoadCefLibrary())
    return -1;
#elif defined(OS_LINUX)
  CefMainArgs main_args(argc, argv);
  int exit_code = CefExecuteProcess(main_args, nullptr, nullptr);
  if (exit_code >= 0) {
    return exit_code;
  }
#endif

  CefUtils::initializeCef();

//  TThreadedServer server(std::make_shared<ServerProcessorFactory>(
//                             std::make_shared<ServerCloneFactory>()),
//                         std::make_shared<TServerSocket>(9090),  // port
//                         std::make_shared<TBufferedTransportFactory>(),
//                         std::make_shared<TBinaryProtocolFactory>());
//
//  Log::debug("Starting the server...");
//  try {
//    server.serve();
//  } catch (TException e) {
//    Log::error("Exception in listening thread");
//    Log::error(e.what());
//  }
//
//  Log::debug("Done, server stopped.");
  return 0;
}
