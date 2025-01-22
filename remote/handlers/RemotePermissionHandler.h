#ifndef JCEF_REMOTEPERMISSIONHANDLER_H
#define JCEF_REMOTEPERMISSIONHANDLER_H

#include "include/cef_permission_handler.h"

class RpcExecutor;

class RemotePermissionHandler : public CefPermissionHandler {
 public:
  explicit RemotePermissionHandler(int bid, std::shared_ptr<RpcExecutor> service);

  bool OnRequestMediaAccessPermission(
      CefRefPtr<CefBrowser> browser,
      CefRefPtr<CefFrame> frame,
      const CefString& requesting_origin,
      uint32_t requested_permissions,
      CefRefPtr<CefMediaAccessCallback> callback) override;

 private:
  const int myBid;
  std::shared_ptr<RpcExecutor> myService;

  IMPLEMENT_REFCOUNTING(CefPermissionHandler);
};

#endif  // JCEF_REMOTEPERMISSIONHANDLER_H
