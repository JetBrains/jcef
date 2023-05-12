#ifndef JCEF_REMOTEQUERYCALLBACK_H
#define JCEF_REMOTEQUERYCALLBACK_H

#include "../RemoteObjectFactory.h"
#include "include/cef_base.h"

// Created when processing MessageRouterHandler::OnQuery and passed into java
// Disposed when java side calls some of callback's methods (or manually from java side)
// TODO: add simple leak protection (link with RemoteMessageRouterHandler, dispose in ~RemoteMessageRouterHandler)
class RemoteQueryCallback  : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteQueryCallback, CefMessageRouterBrowserSide::Callback> {
 public:
  static RemoteQueryCallback * create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefMessageRouterBrowserSide::Callback> delegate);
  
 private:
  explicit RemoteQueryCallback(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefMessageRouterBrowserSide::Callback> delegate, int id);
  IMPLEMENT_REFCOUNTING(RemoteQueryCallback);
};

#endif  // JCEF_REMOTEQUERYCALLBACK_H
