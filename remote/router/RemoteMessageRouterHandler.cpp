#include "RemoteMessageRouterHandler.h"
#include "RemoteQueryCallback.h"
#include "../browser/ClientsManager.h"

// remove to enable tracing
//#define TRACE()

RemoteMessageRouterHandler::RemoteMessageRouterHandler(
    std::shared_ptr<RpcExecutor> service,
    std::shared_ptr<ClientsManager> manager,
    thrift_codegen::RObject peer)
    : RemoteJavaObjectBase(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->ResourceRequestHandler_Dispose(peer.objId);
          }),
      myClientsManager(manager) {
  TRACE();
  Log::trace("new RouterHandler: peerId=%d", peer.objId);
}

RemoteMessageRouterHandler::~RemoteMessageRouterHandler() {
  Log::trace("delete RouterHandler: peerId=%d", myPeerId);
  for (auto cb: myCallbacks) // simple protection for leaking via callbacks
    RemoteQueryCallback::dispose(cb);
}

bool RemoteMessageRouterHandler::OnQuery(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     int64_t query_id,
                     const CefString& request,
                     bool persistent,
                     CefRefPtr<Callback> callback) {
  TRACE();
  const int bid = myClientsManager->findRemoteBrowser(browser);
  if (bid < 0) {
    Log::error("Can't find remote browser by cef-id %d", browser ? browser->GetIdentifier() : -1);
    return false;
  }
  thrift_codegen::RObject rcb = RemoteQueryCallback::create(myService, callback);
  bool handled = myService->exec<bool>([&](RpcExecutor::Service s){
    return s->MessageRouterHandler_onQuery(javaId(), bid, query_id, request, persistent, rcb);
  }, false);
  if (!handled) // NOTE: must delete callback when onQuery returns false
    RemoteQueryCallback::dispose(rcb.objId);
  else
    myCallbacks.insert(rcb.objId);
  return handled;
}

void RemoteMessageRouterHandler::OnQueryCanceled(CefRefPtr<CefBrowser> browser,
                             CefRefPtr<CefFrame> frame,
                             int64_t query_id) {
  TRACE();
  const int bid = myClientsManager->findRemoteBrowser(browser);
  if (bid < 0) {
    Log::error("Can't find remote browser by cef-id %d", browser ? browser->GetIdentifier() : -1);
    return;
  }
  myService->exec([&](RpcExecutor::Service s){
    return s->MessageRouterHandler_onQueryCanceled(javaId(), bid, query_id);
  });
}

