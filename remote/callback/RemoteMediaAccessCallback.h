#ifndef JCEF_REMOTEMEDIAACCESSCALLBACK_H
#define JCEF_REMOTEMEDIAACCESSCALLBACK_H

#include "include/cef_permission_handler.h"
#include "../RemoteObjects.h"

class RemoteMediaAccessCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteMediaAccessCallback, CefMediaAccessCallback> {
public:
  explicit RemoteMediaAccessCallback(int id, const CefRefPtr<CefMediaAccessCallback>& delegate) : RemoteServerObject<RemoteMediaAccessCallback, CefMediaAccessCallback>(id, delegate) {}

private:
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteMediaAccessCallback);
};

#endif  // JCEF_REMOTEMEDIAACCESSCALLBACK_H
