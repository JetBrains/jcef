#ifndef JCEF_REMOTEQUERYCALLBACK_H
#define JCEF_REMOTEQUERYCALLBACK_H

#include "../RemoteObjects.h"
#include "include/cef_base.h"

// Created when processing MessageRouterHandler::OnQuery and passed into java
// Disposed when java side calls some of callback's methods (or manually from java side)
class RemoteQueryCallback  : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteQueryCallback, CefMessageRouterBrowserSide::Callback> {
public:
  explicit RemoteQueryCallback(int id, CefRefPtr<CefMessageRouterBrowserSide::Callback> delegate)
    : RemoteServerObject<RemoteQueryCallback, CefMessageRouterBrowserSide::Callback>(id, delegate) {}

 private:
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteQueryCallback);
};

#endif  // JCEF_REMOTEQUERYCALLBACK_H
