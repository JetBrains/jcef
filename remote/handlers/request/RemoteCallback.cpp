#include "RemoteCallback.h"

RemoteCallback::RemoteCallback(RemoteClientHandler& owner, CefRefPtr<CefCallback> delegate, int id)
    : RemoteServerObject<RemoteCallback, CefCallback>(owner, id, delegate) {}

RemoteCallback * RemoteCallback::create(RemoteClientHandler & owner, CefRefPtr<CefCallback> delegate) {
  return FACTORY.create([&](int id) -> RemoteCallback* {return new RemoteCallback(owner, delegate, id);});
}