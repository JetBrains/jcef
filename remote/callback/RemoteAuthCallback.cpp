#include "RemoteAuthCallback.h"

#include <utility>

RemoteAuthCallback::RemoteAuthCallback(CefRefPtr<CefAuthCallback> delegate, int id)
    : RemoteServerObject<RemoteAuthCallback, CefAuthCallback>(id, std::move(delegate)) {}

thrift_codegen::RObject RemoteAuthCallback::create(CefRefPtr<CefAuthCallback> delegate) {
  return FACTORY.create([&](int id) -> RemoteAuthCallback* {return new RemoteAuthCallback(delegate, id);})->serverId();
}