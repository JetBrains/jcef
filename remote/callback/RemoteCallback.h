#ifndef JCEF_REMOTECALLBACK_H
#define JCEF_REMOTECALLBACK_H

#include "../RemoteObjects.h"
#include "include/cef_base.h"
#include "include/cef_callback.h"

class RemoteCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteCallback, CefCallback> {
 private:
  explicit RemoteCallback(CefRefPtr<CefCallback> delegate, int id) : RemoteServerObject<RemoteCallback, CefCallback>(id, delegate) {}
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteCallback);
};


#endif  // JCEF_REMOTECALLBACK_H
