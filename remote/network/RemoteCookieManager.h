#ifndef JCEF_REMOTECOOKIEMANAGER_H
#define JCEF_REMOTECOOKIEMANAGER_H

#include "../RemoteObjects.h"
#include "include/cef_cookie.h"

// Created by java request (ServerHandler::CookieManager_Create).
// Owned by java peer, disposed by java request (when java-peer is garbage collected)
class RemoteCookieManager : public RemoteServerObject<RemoteCookieManager, CefCookieManager> {
 public:
  static RemoteCookieManager * create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefCookieManager> delegate);

 private:
  std::shared_ptr<RpcExecutor> myService;
  explicit RemoteCookieManager(std::shared_ptr<RpcExecutor> service, int id, CefRefPtr<CefCookieManager> delegate);
};

#endif  // JCEF_REMOTECOOKIEMANAGER_H
