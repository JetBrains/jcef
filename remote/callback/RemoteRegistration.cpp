#include "RemoteRegistration.h"

RemoteRegistration * RemoteRegistration::create(CefRefPtr<CefRegistration> delegate) {
  if (!delegate)
    return nullptr;
  return FACTORY.create([&](int id) -> RemoteRegistration* {return new RemoteRegistration(delegate, id);});
}
