#include "RemoteCallback.h"

#include <utility>

RemoteCallback::RemoteCallback(CefRefPtr<CefCallback> delegate, int id)
    : RemoteServerObject<RemoteCallback, CefCallback>(id, delegate) {}

thrift_codegen::RObject RemoteCallback::create(CefRefPtr<CefCallback> delegate) {
  return FACTORY.create([&](int id) -> RemoteCallback* {return new RemoteCallback(delegate, id);})->serverId();
}