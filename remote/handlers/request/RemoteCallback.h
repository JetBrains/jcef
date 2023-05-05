#ifndef JCEF_REMOTECALLBACK_H
#define JCEF_REMOTECALLBACK_H

#include "../RemoteObjectFactory.h"
#include "include/cef_base.h"
#include "include/cef_callback.h"

class RemoteCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteCallback, CefCallback> {
 public:
  static RemoteCallback * create(RemoteClientHandler & owner, CefRefPtr<CefCallback> delegate);

 private:
  explicit RemoteCallback(RemoteClientHandler& owner, CefRefPtr<CefCallback> delegate, int id);
  IMPLEMENT_REFCOUNTING(RemoteCallback);
};


#endif  // JCEF_REMOTECALLBACK_H
