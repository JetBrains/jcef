#ifndef JCEF_CLIENTSMANAGER_H
#define JCEF_CLIENTSMANAGER_H

#include <memory>
#include <mutex>
#include <map>
#include "include/cef_base.h"

#include "../gen-cpp/shared_types.h"

class RemoteClientHandler;
class ServerHandlerContext;
class CefBrowser;

class ClientsManager {
 public:
  ClientsManager();

  // Returns bid
  int createBrowser(int cid /*id from java*/,
                    std::shared_ptr<ServerHandlerContext> ctx,
                    int handlersMask, const thrift_codegen::RObject& requestContextHandler);
  void startNativeBrowserCreation(int bid, const std::string& url);
  void closeBrowser(int bid);

  void erase(int bid);

  // returns short description of remaining browsers (or empty string when empty browsers set)
  std::string closeAllBrowsers();

  CefRefPtr<CefBrowser> getCefBrowser(int bid);
  CefRefPtr<RemoteClientHandler> getClient(int bid);
  int findRemoteBrowser(CefRefPtr<CefBrowser> browser); // returns bid

 private:

  class ClientsStorage {
   public:
    CefRefPtr<RemoteClientHandler> get(int bid);
    void set(int bid, CefRefPtr<RemoteClientHandler>);
    void erase(int bid);
    int findRemoteBrowser(CefRefPtr<CefBrowser> browser); // returns bid

    // returns short description of remaining browsers (or empty string when empty browsers set)
    std::string closeAll();

    std::recursive_mutex myMutex;
   private:
    std::map<int, CefRefPtr<RemoteClientHandler>> myBid2Client;

    std::string enumClients();
  };

  std::shared_ptr<ClientsStorage> myRemoteClients;
};

#endif  // JCEF_CLIENTSMANAGER_H
