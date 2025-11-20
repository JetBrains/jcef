#include "RemoteMessageRouterHandler.h"
#include "RemoteQueryCallback.h"
#include "../browser/RemoteBrowser.h"
#include "../browser/RemoteFrame.h"

class RemoteBrowser;
const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemoteMessageRouterHandler");
const bool doTraceRequest = getBoolEnv("CEF_SERVER_TRACE_RemoteMessageRouterHandler_Request");

RemoteMessageRouterHandler::RemoteMessageRouterHandler(
    std::shared_ptr<ServerHandlerContext> service,
    thrift_codegen::RObject peer)
    : RemoteJavaObject(
          service,
          peer.objId,
          [=](JavaService service) {
            service->MessageRouterHandler_Dispose(peer.objId);
          }) {
  if (doTrace)
    Log::trace("RemoteMessageRouterHandler: created instance with peerId=%d", peer.objId);

  //Log::trace("new RouterHandler: peerId=%d", peer.objId);
}

RemoteMessageRouterHandler::~RemoteMessageRouterHandler() {
  if (doTrace)
    Log::trace("RemoteMessageRouterHandler: delete instance with peerId=%d", myPeerId);

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
  if (doTrace) {
    Log::trace("RemoteMessageRouterHandler: OnQuery: peerId=%d", myPeerId);
    if (doTraceRequest)
      Log::trace("request:\n%s", request.ToString().c_str());
  }

  std::shared_ptr<RemoteBrowser> rb = RemoteBrowser::findByCefBrowser(browser);
  if (!rb) {
    Log::error("OnQuery: can't find remote browser by cef-id %d", browser ? browser->GetIdentifier() : -1);
    return false;
  }

  RemoteFrame::Holder frm(frame);
  RemoteQueryCallback* rcb = RemoteQueryCallback::wrapDelegate(callback);
  bool handled = myCtx->javaService()->exec<bool>([&](JavaService s){
    return s->MessageRouterHandler_onQuery(javaId(), rb->getBid(), frm.serverId(), query_id, request, persistent, rcb->serverId());
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
  if (doTrace)
    Log::trace("RemoteMessageRouterHandler: OnQueryCanceled: peerId=%d", myPeerId);

  std::shared_ptr<RemoteBrowser> rb = RemoteBrowser::findByCefBrowser(browser);
  if (!rb) {
    Log::error("OnQueryCanceled: can't find remote browser by cef-id %d", browser ? browser->GetIdentifier() : -1);
    return;
  }

  RemoteFrame::Holder frm(frame);
  myCtx->javaService()->exec([&](JavaService s){
    return s->MessageRouterHandler_onQueryCanceled(javaId(), rb->getBid(), frm.serverId(), query_id);
  });
}

