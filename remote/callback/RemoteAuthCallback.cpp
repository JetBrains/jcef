#include "RemoteAuthCallback.h"

RemoteAuthCallback::RemoteAuthCallback(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefAuthCallback> delegate, int id)
    : RemoteServerObject<RemoteAuthCallback, CefAuthCallback>(service, id, delegate) {}

RemoteAuthCallback * RemoteAuthCallback::create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefAuthCallback> delegate) {
  return FACTORY.create([&](int id) -> RemoteAuthCallback* {return new RemoteAuthCallback(service, delegate, id);});
}