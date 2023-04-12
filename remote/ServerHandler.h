#ifndef JCEF_SERVERHANDLER_H
#define JCEF_SERVERHANDLER_H

#include "./gen-cpp/Server.h"

#include "log/Log.h"
#include "RemoteClientHandler.h"
#include "Utils.h"

// TODO: make thread-safe

// Used per connection (destroyed when connection closed)
class ServerHandler : public thrift_codegen::ServerIf {
 public:
  ~ServerHandler();

  //
  // ServerIf
  //
  int32_t connect() override;
  int32_t createBrowser(int cid) override;
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
