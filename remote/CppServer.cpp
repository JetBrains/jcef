#include <thrift/concurrency/ThreadFactory.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include <iostream>
#include <stdexcept>

#include "./gen-cpp/ClientHandlers.h"
#include "./gen-cpp/Server.h"

#include "CefUtils.h"
#include "RemoteClientHandler.h"
#include "RemoteRenderHandler.h"
#include "RemoteLifespanHandler.h"
#include "log/Log.h"

#include "include/cef_app.h"

using namespace std;
using namespace apache::thrift;
using namespace apache::thrift::concurrency;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::server;

using namespace remote;


class ServerHandler : public ServerIf {
 private:
  std::shared_ptr<ClientHandlersClient> myClient = nullptr;
  std::shared_ptr<TTransport> mySocket;
  std::shared_ptr<TTransport> myTransport;
  std::shared_ptr<TProtocol> myProtocol;

  std::vector<CefRefPtr<RemoteClientHandler>> myRemoteHandlers;

 public:
  ServerHandler() {}

  int32_t connect() override {
    static int cid = 0;
    Log::debug("connected new client with cid=%d", cid);

    // Connect to client's side (for cef-callbacks execution on java side)
    if (myClient == nullptr) {
      mySocket = std::make_shared<TSocket>("localhost", 9091);
      myTransport = std::make_shared<TBufferedTransport>(mySocket);
      myProtocol = std::make_shared<TBinaryProtocol>(myTransport);
      myClient = std::make_shared<ClientHandlersClient>(myProtocol);

      try {
        myTransport->open();
        const int32_t backwardCid = myClient->connect();
        Log::debug("\tbackward connection to client established [%d]", backwardCid);
      } catch (TException& tx) {
        Log::error(tx.what());
      }
    }

    return cid++;
  }

  int32_t createBrowser() override {
    const int bid = myRemoteHandlers.size();
    CefRefPtr<RemoteRenderHandler> renderHandler = new RemoteRenderHandler(myClient, bid);
    CefRefPtr<RemoteClientHandler> clienthandler = new RemoteClientHandler(renderHandler);

    CefWindowInfo windowInfo;
    windowInfo.SetAsWindowless(0);

    CefBrowserSettings settings;
    CefString strUrl("www.google.com");

    bool result = CefBrowserHost::CreateBrowser(windowInfo, clienthandler, strUrl,
                                                settings, nullptr, nullptr);
    if (!result) {
      Log::error( "failed to create browser with bid=%d", bid);
      return -1;
    }
    Log::debug("browser successfully created, bid=%d", bid);

    myRemoteHandlers.push_back(clienthandler);
    return bid;
  }

  void invoke(const int32_t bid, const std::string& method, const std::string& buffer) override {
    if (bid >= myRemoteHandlers.size()) {
      Log::error("bid %d > myRemoteHandlers.size() %d", bid, myRemoteHandlers.size());
      return;
    }

    auto ch = myRemoteHandlers[bid];
    RemoteLifespanHandler * rsh = (RemoteLifespanHandler *)(ch->GetLifeSpanHandler()).get();
    auto browser = rsh->getBrowser();

    if (browser == nullptr) {
      Log::error("null browser, bid=%d", bid);
      return;
    }

    if (method.compare("wasresized") == 0) {
      browser->GetHost()->WasResized();
    } else if (method.compare("sendmouseevent") == 0) {
      const int len = buffer.size();
      if (len < 4) {
        Log::error("sendmouseevent, len %d < 4", len);
        return;
      }

      const int32_t * p = (const int32_t *)buffer.c_str();
      int event_type = *(p++);
      int modifiers = *(p++);

      CefMouseEvent cef_event;
      cef_event.x = *(p++);
      cef_event.y = *(p++);

      // TODO: read modifiers and other params
      CefBrowserHost::MouseButtonType cef_mbt = MBT_LEFT;
      browser->GetHost()->SendMouseClickEvent(cef_event, cef_mbt, event_type == 0, 1);
    } else if (method.compare("sendkeyevent") == 0) {
      CefKeyEvent cef_event;
      // TODO: read modifiers and other params
      browser->GetHost()->SendKeyEvent(cef_event);
    }
  }

  void log(const std::string& msg) override {
    Log::info("received message from client: %s", msg.c_str());
  }

  void close() { myTransport->close(); }

 protected:
};

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
