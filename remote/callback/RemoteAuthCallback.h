#ifndef JCEF_REMOTEAUTHCALLBACK_H
#define JCEF_REMOTEAUTHCALLBACK_H

#include "../RemoteObjectFactory.h"
#include "include/cef_auth_callback.h"
#include "include/cef_base.h"

class RemoteAuthCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteAuthCallback, CefAuthCallback> {
 public:
  static RemoteAuthCallback * create(RemoteClientHandler & owner, CefRefPtr<CefAuthCallback> delegate);

 private:
  explicit RemoteAuthCallback(RemoteClientHandler& owner, CefRefPtr<CefAuthCallback> delegate, int id);
  IMPLEMENT_REFCOUNTING(RemoteAuthCallback);
};


#endif  // JCEF_REMOTEAUTHCALLBACK_H
