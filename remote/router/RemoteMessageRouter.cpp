#include "RemoteMessageRouter.h"
#include "RemoteMessageRouterHandler.h"
#include "../CefUtils.h"

const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemoteMessageRouter");

RemoteMessageRouter::RemoteMessageRouter(std::shared_ptr<ServerHandlerContext> ctx, int id, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config)
    : RemoteServerObject<RemoteMessageRouter, CefMessageRouter>(id, delegate), myCtx(ctx), myConfig(config)
{
  if (doTrace)
    Log::trace("RemoteMessageRouter: created instance with id=%d, config: query='%s' cancel='%s'", id, config.js_query_function.ToString().c_str(), config.js_cancel_function.ToString().c_str());
}

RemoteMessageRouter * RemoteMessageRouter::create(std::shared_ptr<ServerHandlerContext> ctx, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config) {
  return FACTORY.create([&](int id) -> RemoteMessageRouter* {return new RemoteMessageRouter(ctx, id, delegate, config);});
}

RemoteMessageRouter * RemoteMessageRouter::create(std::shared_ptr<ServerHandlerContext> ctx, const std::string &query, const std::string &cancel) {
    CefMessageRouterConfig config;
    config.js_query_function = query;
    config.js_cancel_function = cancel;
    CefRefPtr<CefMessageRouterBrowserSide> msgRouter = CefMessageRouterBrowserSide::Create(config);
    RemoteMessageRouter * result = create(ctx, msgRouter, config);
    return result;
}

void RemoteMessageRouter::AddRemoteHandler(const thrift_codegen::RObject& handler, bool first) {
  if (doTrace)
    Log::trace("RemoteMessageRouter: add handler with objId=%d, first=%d", handler.objId, first ? 1 : 0);

  std::shared_ptr<RemoteMessageRouterHandler> rmrh = std::make_shared<RemoteMessageRouterHandler>(myCtx, handler);
  myDelegate->AddHandler(rmrh.get(), first);

  Lock lock(myMutex);
  myHandlers[handler.objId] = rmrh;
}

void RemoteMessageRouter::RemoveRemoteHandler(const thrift_codegen::RObject& handler) {
  if (doTrace)
    Log::trace("RemoteMessageRouter: remove handler with objId=%d", handler.objId);

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
  else if (doTrace)
    Log::trace("RemoteMessageRouter: for id=%d found handler (with peer's objId=%d)", objId, rmrh->javaId().objId);

  return rmrh;
}

