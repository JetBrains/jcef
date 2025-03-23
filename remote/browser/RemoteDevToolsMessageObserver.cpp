#include "RemoteDevToolsMessageObserver.h"

#include "include/cef_browser.h"

#include "../browser/ClientsManager.h"
#include "../ServerHandlerContext.h"

RemoteDevToolsMessageObserver::RemoteDevToolsMessageObserver(
    std::shared_ptr<ClientsManager> clientsManager,
    std::shared_ptr<ServerHandlerContext> ctx,
    thrift_codegen::RObject peer)
    : RemoteJavaObject<RemoteDevToolsMessageObserver>(
          ctx,
          peer.objId,
          [=](JavaService service) {
            service->DevToolsMessageObserver_Dispose(peer.objId);
            Log::trace("Disposed DevToolsMessageObserver, peer-id=%d", peer.objId);
          }), myClientsManager(clientsManager) {
  Log::trace("Created DevToolsMessageObserver, peer-id=%d", peer.objId);
}

void RemoteDevToolsMessageObserver::OnDevToolsEvent(
    CefRefPtr<CefBrowser> browser,
    const CefString& method,
    const void* params,
    size_t params_size
) {
  const int bid = myClientsManager->findRemoteBrowser(browser);
  if (bid < 0) {
    Log::error("RemoteDevToolsMessageObserver::OnDevToolsEvent: can't find remove browser by native identifier %d", browser->GetIdentifier());
    return;
  }

  std::string strParams(static_cast<const char*>(params), params_size);
  myCtx->javaService()->exec([&](JavaService s){
    s->DevToolsMessageObserver_OnDevToolsEvent(myPeerId, bid, method, strParams);
  });
}

void RemoteDevToolsMessageObserver::OnDevToolsMethodResult(
    CefRefPtr<CefBrowser> browser,
    int message_id,
    bool success,
    const void* result,
    size_t result_size
) {
  const int bid = myClientsManager->findRemoteBrowser(browser);
  if (bid < 0) {
    Log::error("RemoteDevToolsMessageObserver::OnDevToolsMethodResult: can't find remove browser by native identifier %d", browser->GetIdentifier());
    return;
  }

  std::string strResult(static_cast<const char*>(result), result_size);
  myCtx->javaService()->exec([&](JavaService s){
    s->DevToolsMessageObserver_OnDevToolsMethodResult(myPeerId, bid, message_id, success, strResult);
  });
}
