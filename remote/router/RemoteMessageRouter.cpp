#include "RemoteMessageRouter.h"
#include "RemoteMessageRouterHandler.h"
#include "../CefUtils.h"

const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemoteMessageRouter");

RemoteMessageRouter::RemoteMessageRouter(int id, std::shared_ptr<ServerHandlerContext> ctx, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config)
    : RemoteServerObject<RemoteMessageRouter, CefMessageRouter>(id, delegate), myCtx(ctx), myConfig(config)
{
  if (doTrace)
    Log::trace("RemoteMessageRouter: created instance with id=%d, config: query='%s' cancel='%s'", id, config.js_query_function.ToString().c_str(), config.js_cancel_function.ToString().c_str());
}

std::shared_ptr<RemoteMessageRouter> RemoteMessageRouter::create(std::shared_ptr<ServerHandlerContext> ctx, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config) {
  return FACTORY.create(ctx, delegate, config);
}

std::shared_ptr<RemoteMessageRouter> RemoteMessageRouter::create(std::shared_ptr<ServerHandlerContext> ctx, const std::string &query, const std::string &cancel) {
    CefMessageRouterConfig config;
    config.js_query_function = query;
    config.js_cancel_function = cancel;
    CefRefPtr<CefMessageRouterBrowserSide> msgRouter = CefMessageRouterBrowserSide::Create(config);
    std::shared_ptr<RemoteMessageRouter> result = create(ctx, msgRouter, config);
    return result;
}

void RemoteMessageRouter::AddRemoteHandler(const thrift_codegen::RObject& handler, bool first) {
  if (doTrace)
    Log::trace("RemoteMessageRouter: add handler with uid=%d, first=%d", handler.uid, first ? 1 : 0);

  std::shared_ptr<RemoteMessageRouterHandler> rmrh = std::make_shared<RemoteMessageRouterHandler>(myCtx, handler);
  myDelegate->AddHandler(rmrh.get(), first);

  Lock lock(myMutex);
  myHandlers[handler.uid] = rmrh;
}

void RemoteMessageRouter::RemoveRemoteHandler(const thrift_codegen::RObject& handler) {
  if (doTrace)
    Log::trace("RemoteMessageRouter: remove handler with uid=%d", handler.uid);

  std::shared_ptr<RemoteMessageRouterHandler> rmrh;
  {
    Lock lock(myMutex);
    rmrh = myHandlers[handler.uid];
    myHandlers[handler.uid] = nullptr;
  }
  if (rmrh)
    myDelegate->RemoveHandler(rmrh.get());
  else
    Log::error("Can't find (to remove) RemoteMessageRouterHandler %d", handler.uid);
}

std::shared_ptr<RemoteMessageRouterHandler> RemoteMessageRouter::FindRemoteHandler(int uid) {

  std::shared_ptr<RemoteMessageRouterHandler> rmrh;
  {
    Lock lock(myMutex);
    rmrh = myHandlers[uid];
  }
  if (!rmrh)
    Log::error("Can't find RemoteMessageRouterHandler %d", uid);
  else if (doTrace)
    Log::trace("RemoteMessageRouter: for id=%d found handler (with peer's uid=%d)", uid, rmrh->javaId().uid);

  return rmrh;
}

