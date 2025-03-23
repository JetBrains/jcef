#ifndef JCEF_REMOTEMEDIAACCESSCALLBACK_H
#define JCEF_REMOTEMEDIAACCESSCALLBACK_H

#include "include/cef_permission_handler.h"
#include "../RemoteObjects.h"

class RemoteMediaAccessCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteMediaAccessCallback, CefMediaAccessCallback> {
 private:
  explicit RemoteMediaAccessCallback(const CefRefPtr<CefMediaAccessCallback>& delegate, int id) : RemoteServerObject<RemoteMediaAccessCallback, CefMediaAccessCallback>(id, delegate) {}
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteMediaAccessCallback);
};

#endif  // JCEF_REMOTEMEDIAACCESSCALLBACK_H
