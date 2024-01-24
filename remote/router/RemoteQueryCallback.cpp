#include "RemoteQueryCallback.h"

RemoteQueryCallback::RemoteQueryCallback(CefRefPtr<CefMessageRouterBrowserSide::Callback> delegate, int id)
    : RemoteServerObject<RemoteQueryCallback, CefMessageRouterBrowserSide::Callback>(id, delegate) {}

thrift_codegen::RObject RemoteQueryCallback::create(CefRefPtr<CefMessageRouterBrowserSide::Callback> delegate) {
  return FACTORY.create([&](int id) -> RemoteQueryCallback* {return new RemoteQueryCallback(delegate, id);})->serverId();
}
