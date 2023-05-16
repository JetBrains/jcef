#ifndef JCEF_CLIENTSMANAGER_H
#define JCEF_CLIENTSMANAGER_H

#include <memory>
#include "include/cef_base.h"

class RemoteClientHandler;
class RpcExecutor;
class MessageRoutersManager;
class CefBrowser;

class ClientsManager {
 public:
  ClientsManager();

  // TODO: make all API thread-safe

  // Returns bid
  int createBrowser(int cid/*id from java*/, std::shared_ptr<RpcExecutor> service, std::shared_ptr<MessageRoutersManager> routersManager, const std::string& url);
  void closeBrowser(const int32_t bid);
  void closeAllBrowsers();

  CefRefPtr<CefBrowser> getCefBrowser(int bid);
  int findRemoteBrowser(CefRefPtr<CefBrowser> browser);

  CefRefPtr<RemoteClientHandler> getClient(int bid);
  void disposeClient(int bid);

 private:
  std::shared_ptr<std::vector<CefRefPtr<RemoteClientHandler>>> myRemoteClients;
};

#endif  // JCEF_CLIENTSMANAGER_H
