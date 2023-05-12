#include "RemoteMessageRouter.h"
#include "RemoteMessageRouterHandler.h"

// remove to enable tracing
#define TRACE()

RemoteMessageRouter::RemoteMessageRouter(std::shared_ptr<RpcExecutor> service, int id, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config)
    : RemoteServerObject<RemoteMessageRouter, CefMessageRouter>(service, id, delegate), myConfig(config) {}

RemoteMessageRouter * RemoteMessageRouter::create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config) {
  return FACTORY.create([&](int id) -> RemoteMessageRouter* {return new RemoteMessageRouter(service, id, delegate, config);});
}

void RemoteMessageRouter::AddRemoteHandler(std::shared_ptr<ClientsManager> manager, const thrift_codegen::RObject& handler, bool first) {
  TRACE();
  RemoteMessageRouterHandler * rmrh = RemoteMessageRouterHandler::create(myService, manager, handler);
  if (rmrh == nullptr) {
    Log::error("Can't create RemoteMessageRouterHandler %d", handler.objId);
    return;
  }

  myDelegate->AddHandler(rmrh, first);
}

void RemoteMessageRouter::RemoveRemoteHandler(const thrift_codegen::RObject& handler) {
  TRACE();
  RemoteMessageRouterHandler * rmrh = RemoteMessageRouterHandler::find(handler.objId);
  if (rmrh != nullptr) {
    myDelegate->RemoveHandler(rmrh);
  } else
    Log::error("Can't find RemoteMessageRouterHandler %d", handler.objId);

  RemoteMessageRouterHandler::dispose(handler.objId);
}
