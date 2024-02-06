#include "RemoteMessageRouter.h"
#include "RemoteMessageRouterHandler.h"
#include "../CefUtils.h"

// remove to enable tracing
#ifdef TRACE
#undef TRACE
#define TRACE()
#endif

RemoteMessageRouter::RemoteMessageRouter(std::shared_ptr<RpcExecutor> service, int id, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config)
    : RemoteServerObject<RemoteMessageRouter, CefMessageRouter>(id, delegate), myService(service), myConfig(config)
{
    // LogNdc ndc(string_format("Create router <%s | %s>", config.js_query_function.ToString().c_str(), config.js_cancel_function.ToString().c_str()), "", 0, true);
}

RemoteMessageRouter * RemoteMessageRouter::create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config) {
  return FACTORY.create([&](int id) -> RemoteMessageRouter* {return new RemoteMessageRouter(service, id, delegate, config);});
}

void RemoteMessageRouter::AddRemoteHandler(std::shared_ptr<ClientsManager> manager, const thrift_codegen::RObject& handler, bool first) {
  TRACE();
  std::shared_ptr<RemoteMessageRouterHandler> rmrh = std::make_shared<RemoteMessageRouterHandler>(myService, manager, handler);
  myDelegate->AddHandler(rmrh.get(), first);

  Lock lock(myMutex);
  myHandlers[handler.objId] = rmrh;
}

void RemoteMessageRouter::RemoveRemoteHandler(const thrift_codegen::RObject& handler) {
  TRACE();
  std::shared_ptr<RemoteMessageRouterHandler> rmrh;
  {
    Lock lock(myMutex);
    rmrh = myHandlers[handler.objId];
    myHandlers[handler.objId] = nullptr;
  }
  if (rmrh)
    myDelegate->RemoveHandler(rmrh.get());
  else
    Log::error("Can't find (to remove) RemoteMessageRouterHandler %d", handler.objId);
}

std::shared_ptr<RemoteMessageRouterHandler> RemoteMessageRouter::FindRemoteHandler(int objId) {
  std::shared_ptr<RemoteMessageRouterHandler> rmrh;
  {
    Lock lock(myMutex);
    rmrh = myHandlers[objId];
  }
  if (!rmrh)
    Log::error("Can't find RemoteMessageRouterHandler %d", objId);
  return rmrh;
}

