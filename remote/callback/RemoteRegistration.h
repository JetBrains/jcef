#ifndef JCEF_REMOTEREGISTRATION_H
#define JCEF_REMOTEREGISTRATION_H

#include "../RemoteObjects.h"
#include "include/cef_base.h"
#include "include/cef_registration.h"

class RemoteRegistration : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteRegistration, CefRegistration> {
 public:
  explicit RemoteRegistration(int id, CefRefPtr<CefRegistration> delegate) : RemoteServerObject<RemoteRegistration, CefRegistration>(id, delegate) {}
  static std::shared_ptr<RemoteRegistration> create(CefRefPtr<CefRegistration> delegate);

 private:
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteRegistration);
};

#endif  // JCEF_REMOTEREGISTRATION_H
