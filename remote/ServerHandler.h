#ifndef JCEF_SERVERHANDLER_H
#define JCEF_SERVERHANDLER_H

#include "./gen-cpp/Server.h"
#include "./gen-cpp/ClientHandlers.h"

#include "include/cef_app.h"

#include "log/Log.h"
#include "RemoteClientHandler.h"

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

using namespace thrift_codegen;

class BackwardConnection {
  std::shared_ptr<ClientHandlersClient> myClientHandlers = nullptr;
  std::shared_ptr<TTransport> myTransport;

 public:
  BackwardConnection();

  void close();
  std::shared_ptr<ClientHandlersClient> getHandlersService() { return myClientHandlers; }
};

// TODO: make thread-safe

// Used per connection (destroyed when connection closed)
class ServerHandler : public ServerIf {
 public:
  ~ServerHandler();

  //
  // ServerIf
  //
  int32_t connect() override;
  int32_t createBrowser() override;
  void closeBrowser(std::string& _return, const int32_t bid) override;
  void invoke(const int32_t bid, const std::string& method, const std::string& buffer) override;

  void log(const std::string& msg) override {
    Log::info("received message from client: %s", msg.c_str());
  }

  //
  // Public API
  //
  std::shared_ptr<BackwardConnection> getBackwardConnection() { return myBackwardConnection; }

 private:
  CefRefPtr<CefBrowser> getBrowser(int bid);
  void closeAllBrowsers();

  std::vector<CefRefPtr<RemoteClientHandler>> myRemoteBrowsers;
  std::shared_ptr<BackwardConnection> myBackwardConnection;
};

#endif  // JCEF_SERVERHANDLER_H
