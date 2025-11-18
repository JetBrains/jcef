#include "RemoteDevToolsMessageObserver.h"

#include "RemoteBrowser.h"
#include "include/cef_browser.h"

#include "../ServerHandlerContext.h"

RemoteDevToolsMessageObserver::RemoteDevToolsMessageObserver(
    std::shared_ptr<ServerHandlerContext> ctx,
    thrift_codegen::RObject peer)
    : RemoteJavaObject<RemoteDevToolsMessageObserver>(
          ctx,
          peer.objId,
          [=](JavaService service) {
            service->DevToolsMessageObserver_Dispose(peer.objId);
            Log::trace("Disposed DevToolsMessageObserver, peer-id=%d", peer.objId);
          }) {
  Log::trace("Created DevToolsMessageObserver, peer-id=%d", peer.objId);
}

void RemoteDevToolsMessageObserver::OnDevToolsEvent(
    CefRefPtr<CefBrowser> browser,
    const CefString& method,
    const void* params,
    size_t params_size
) {
  std::shared_ptr<RemoteBrowser> rb = RemoteBrowser::findByCefBrowser(browser);
  if (!rb) {
    Log::error("RemoteDevToolsMessageObserver::OnDevToolsEvent: can't find RemoteBrowser by native identifier %d", browser->GetIdentifier());
    return;
  }

  std::string strParams(static_cast<const char*>(params), params_size);
  myCtx->javaService()->exec([&](JavaService s){
    s->DevToolsMessageObserver_OnDevToolsEvent(myPeerId, rb->getBid(), method, strParams);
  });
}

void RemoteDevToolsMessageObserver::OnDevToolsMethodResult(
    CefRefPtr<CefBrowser> browser,
    int message_id,
    bool success,
    const void* result,
    size_t result_size
) {
  std::shared_ptr<RemoteBrowser> rb = RemoteBrowser::findByCefBrowser(browser);
  if (!rb) {
    Log::error("RemoteDevToolsMessageObserver::OnDevToolsMethodResult: can't find RemoteBrowser by native identifier %d", browser->GetIdentifier());
    return;
  }

  std::string strResult(static_cast<const char*>(result), result_size);
  myCtx->javaService()->exec([&](JavaService s){
    s->DevToolsMessageObserver_OnDevToolsMethodResult(myPeerId, rb->getBid(), message_id, success, strResult);
  });
}
