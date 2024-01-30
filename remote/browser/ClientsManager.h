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
                    const std::string& url);
  void closeBrowser(const int32_t bid);
  void closeAllBrowsers();

  CefRefPtr<CefBrowser> getCefBrowser(int bid);
  int findRemoteBrowser(CefRefPtr<CefBrowser> browser);

 private:
  std::recursive_mutex myMutex;
  std::map<int, CefRefPtr<RemoteClientHandler>> myRemoteClients;
};

#endif  // JCEF_CLIENTSMANAGER_H
