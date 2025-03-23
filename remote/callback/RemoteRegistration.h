#ifndef JCEF_REMOTEREGISTRATION_H
#define JCEF_REMOTEREGISTRATION_H

#include "../RemoteObjects.h"
#include "include/cef_base.h"
#include "include/cef_registration.h"

class RemoteRegistration : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteRegistration, CefRegistration> {
 public:
  static RemoteRegistration * create(CefRefPtr<CefRegistration> delegate);

 private:
  explicit RemoteRegistration(CefRefPtr<CefRegistration> delegate, int id) : RemoteServerObject<RemoteRegistration, CefRegistration>(id, delegate) {}
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteRegistration);
};

#endif  // JCEF_REMOTEREGISTRATION_H
