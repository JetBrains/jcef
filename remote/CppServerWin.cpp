//
// Created by khari on 6/20/2023.
//
#include <windows.h>

#include "include/cef_app.h"
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TPipeServer.h>
#include <thrift/transport/TPipe.h>
#include <thrift/transport/TTransportUtils.h>

#include <boost/filesystem.hpp>

#include "CefUtils.h"
#include "ServerHandler.h"
#include "MyTPipeServer.h"
#include "MyTPipe.h"

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

namespace {
class ServerHandlerTest : public thrift_codegen::ServerNull {
  int32_t arg = -1;
  int32_t connect(const int32_t backwardConnectionPort,
                  const std::vector<std::string>& cmdLineArgs,
                  const std::map<std::string, std::string>& settings) override {
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
  ServerIf* getHandler(
      const ::apache::thrift::TConnectionInfo& connInfo) override {
    return new ServerHandlerTest;
  }
  void releaseHandler(ServerIf* handler) override { delete handler; }
};
}

bool _testPipeNativeServer() {
  boost::filesystem::path pipePath = boost::filesystem::temp_directory_path().append("server_pipe").lexically_normal();
  std::remove(pipePath.string().c_str());

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

//  TServerSocket * socket = new TServerSocket(pipePath.string().c_str());
//  std::shared_ptr<TServerTransport> transport(socket);
  std::shared_ptr<MyTPipeServer> transport = std::make_shared<MyTPipeServer>("\\\\.\\pipe\\server_pipe");
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
  try {
    //std::shared_ptr<TTransport> transport = std::make_shared<TSocket>("\\\\.\\pipe\\server_pipe");
    std::shared_ptr<MyTPipe> transport = std::make_shared<MyTPipe>("\\\\.\\pipe\\client_pipe");
    std::shared_ptr<ClientHandlersClient> service =
        std::make_shared<ClientHandlersClient>(std::make_shared<TBinaryProtocol>(transport));

    if (!transport->isOpen())
      transport->open();

    const int32_t cid = service->connect();
    Log::debug("Test connection to java server established, cid=%d", cid);
    service->log("This message is sent via named pipe.");
    transport->close();
    CancelIoEx(transport->getPipeHandle(), NULL);
    CloseHandle(transport->getPipeHandle());
  } catch (TException& tx) {
    fprintf(stderr, tx.what());
    return false;
  }
  return true;
}


int main() {
  HINSTANCE hi = GetModuleHandle (0);

  Log::init(LEVEL_TRACE);
  setThreadName("main");
  _testPipeNativeClient();
  //_testPipeNativeServer();
  if (true)
    return 0;

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
