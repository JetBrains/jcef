#ifndef JCEF_REMOTEAUTHCALLBACK_H
#define JCEF_REMOTEAUTHCALLBACK_H

#include "../RemoteObjects.h"
#include "include/cef_auth_callback.h"
#include "include/cef_base.h"

class RemoteAuthCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteAuthCallback, CefAuthCallback> {
 private:
  explicit RemoteAuthCallback(CefRefPtr<CefAuthCallback> delegate, int id)
      : RemoteServerObject<RemoteAuthCallback, CefAuthCallback>(id, delegate) {}

  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteAuthCallback);
};

#endif  // JCEF_REMOTEAUTHCALLBACK_H
