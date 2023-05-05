#include "RemoteAuthCallback.h"

RemoteAuthCallback::RemoteAuthCallback(RemoteClientHandler& owner, CefRefPtr<CefAuthCallback> delegate, int id)
    : RemoteServerObject<RemoteAuthCallback, CefAuthCallback>(owner, id, delegate) {}

RemoteAuthCallback * RemoteAuthCallback::create(RemoteClientHandler & owner, CefRefPtr<CefAuthCallback> delegate) {
  return FACTORY.create([&](int id) -> RemoteAuthCallback* {return new RemoteAuthCallback(owner, delegate, id);});
}