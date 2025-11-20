#ifndef JCEF_REMOTEAUTHCALLBACK_H
#define JCEF_REMOTEAUTHCALLBACK_H

#include "../RemoteObjects.h"
#include "include/cef_auth_callback.h"
#include "include/cef_base.h"

class RemoteAuthCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteAuthCallback, CefAuthCallback> {
public:
  explicit RemoteAuthCallback(int id, CefRefPtr<CefAuthCallback> delegate)
    : RemoteServerObject<RemoteAuthCallback, CefAuthCallback>(id, delegate) {}

private:
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteAuthCallback);
};

#endif  // JCEF_REMOTEAUTHCALLBACK_H
