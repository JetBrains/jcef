#include "RemoteMessageRouter.h"
#include "RemoteMessageRouterHandler.h"
#include "../CefUtils.h"

const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemoteMessageRouter");

RemoteMessageRouter::RemoteMessageRouter(std::shared_ptr<ServerHandlerContext> ctx, int id, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config)
    : RemoteServerObject<RemoteMessageRouter, CefMessageRouter>(id, delegate), myCtx(ctx), myConfig(config)
{
  TRACE();
}

RemoteMessageRouter * RemoteMessageRouter::create(std::shared_ptr<ServerHandlerContext> ctx, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config) {
  FACTORY_TRACE("CEF_SERVER_OBJTRACE_MessageRouter", "RemoteMessageRouter");
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
  TRACE();
  std::shared_ptr<RemoteMessageRouterHandler> rmrh = std::make_shared<RemoteMessageRouterHandler>(myCtx, handler);
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
  TRACE();
  std::shared_ptr<RemoteMessageRouterHandler> rmrh;
  {
    Lock lock(myMutex);
    rmrh = myHandlers[objId];
  }
  if (!rmrh)
    Log::error("Can't find RemoteMessageRouterHandler %d", objId);
  return rmrh;
}

