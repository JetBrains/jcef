#include "RemoteRegistration.h"

std::shared_ptr<RemoteRegistration> RemoteRegistration::create(CefRefPtr<CefRegistration> delegate) {
  if (!delegate)
    return nullptr;
  return FACTORY.create(delegate);
}
