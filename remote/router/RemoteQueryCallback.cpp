#include "RemoteQueryCallback.h"

RemoteQueryCallback::RemoteQueryCallback(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefMessageRouterBrowserSide::Callback> delegate, int id)
    : RemoteServerObject<RemoteQueryCallback, CefMessageRouterBrowserSide::Callback>(service, id, delegate) {}

thrift_codegen::RObject RemoteQueryCallback::create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefMessageRouterBrowserSide::Callback> delegate) {
  return FACTORY.create([&](int id) -> RemoteQueryCallback* {return new RemoteQueryCallback(service, delegate, id);})->serverId();
}
