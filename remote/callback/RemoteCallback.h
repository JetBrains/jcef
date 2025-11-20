#ifndef JCEF_REMOTECALLBACK_H
#define JCEF_REMOTECALLBACK_H

#include "../RemoteObjects.h"
#include "include/cef_base.h"
#include "include/cef_callback.h"

class RemoteCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteCallback, CefCallback> {
public:
  explicit RemoteCallback(int id, CefRefPtr<CefCallback> delegate) : RemoteServerObject<RemoteCallback, CefCallback>(id, delegate) {}

private:
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteCallback);
};


#endif  // JCEF_REMOTECALLBACK_H
