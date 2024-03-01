#include "RemoteMessageRouterHandler.h"
#include "RemoteQueryCallback.h"
#include "../browser/ClientsManager.h"
#include "../browser/RemoteFrame.h"

// remove to enable tracing
#ifdef TRACE
#undef TRACE
#define TRACE()
#endif

RemoteMessageRouterHandler::RemoteMessageRouterHandler(
    std::shared_ptr<RpcExecutor> service,
    std::shared_ptr<ClientsManager> manager,
    thrift_codegen::RObject peer)
    : RemoteJavaObject(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->MessageRouterHandler_Dispose(peer.objId);
          }),
      myClientsManager(manager) {
  TRACE();
  //Log::trace("new RouterHandler: peerId=%d", peer.objId);
}

RemoteMessageRouterHandler::~RemoteMessageRouterHandler() {
  //Log::trace("delete RouterHandler: peerId=%d", myPeerId);
  for (auto cb: myCallbacks) // simple protection for leaking via callbacks
    RemoteQueryCallback::dispose(cb);
}

///
/// Executed when a new query is received. |query_id| uniquely identifies
/// the query for the life span of the router. Return true to handle the
/// query or false to propagate the query to other registered handlers, if
/// any. If no handlers return true from this method then the query will be
/// automatically canceled with an error code of -1 delivered to the
/// JavaScript onFailure callback. If this method returns true then a
/// Callback method must be executed either in this method or asynchronously
/// to complete the query.
///
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
  RemoteFrame::Holder frm(frame);
  RemoteQueryCallback* rcb = RemoteQueryCallback::wrapDelegate(callback);
  bool handled = myService->exec<bool>([&](RpcExecutor::Service s){
    return s->MessageRouterHandler_onQuery(javaId(), bid, frm.get()->serverIdWithMap(), query_id, request, persistent, rcb->serverId());
  }, false);
  if (!handled) // NOTE: must delete callback when onQuery returns false
    RemoteQueryCallback::dispose(rcb->getId());
  else
    myCallbacks.insert(rcb->getId()); // Callback will be disposed with RemoteMessageRouterHandler (just for insurance)
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
  RemoteFrame::Holder frm(frame);
  myService->exec([&](RpcExecutor::Service s){
    return s->MessageRouterHandler_onQueryCanceled(javaId(), bid, frm.get()->serverIdWithMap(), query_id);
  });
}

