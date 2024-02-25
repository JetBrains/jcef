#ifndef JCEF_CLIENTSMANAGER_H
#define JCEF_CLIENTSMANAGER_H

#include <memory>
#include <mutex>
#include <map>
#include "include/cef_base.h"

class RemoteClientHandler;
class RpcExecutor;
class MessageRoutersManager;
class CefBrowser;

class ClientsManager {
 public:
  ClientsManager();

  // Returns bid
  int createBrowser(int cid /*id from java*/,
                    std::shared_ptr<RpcExecutor> service,
                    std::shared_ptr<RpcExecutor> serviceIO,
                    std::shared_ptr<MessageRoutersManager> routersManager,
                    int handlersMask);
  void startBrowserCreation(int bid, const std::string& url);
  void closeBrowser(int bid);

  // returns short description of remaining browsers (or empty string when empty browsers set)
  std::string closeAllBrowsers();

  CefRefPtr<CefBrowser> getCefBrowser(int bid);
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
