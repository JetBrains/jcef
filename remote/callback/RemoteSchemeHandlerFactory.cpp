#include "RemoteSchemeHandlerFactory.h"
#include "../network/RemoteResourceHandler.h"
#include "../network/RemoteRequest.h"
#include "../browser/ClientsManager.h"
#include "../browser/RemoteFrame.h"

RemoteSchemeHandlerFactory::RemoteSchemeHandlerFactory(
    std::shared_ptr<ClientsManager> clientsManager,
    std::shared_ptr<RpcExecutor> service,
    thrift_codegen::RObject peer)
    : RemoteJavaObject<RemoteSchemeHandlerFactory>(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->SchemeHandlerFactory_Dispose(peer.objId);
            Log::trace("Disposed SchemeHandlerFactory, peer-id=%d", peer.objId);
          }), myClientsManager(clientsManager) {
  Log::trace("Created SchemeHandlerFactory, peer-id=%d", peer.objId);
}

CefRefPtr<CefResourceHandler> RemoteSchemeHandlerFactory::Create(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    const CefString& scheme_name,
    CefRefPtr<CefRequest> request
) {
  const int bid = myClientsManager->findRemoteBrowser(browser);
  if (bid < 0) {
    Log::error("RemoteSchemeHandlerFactory::Create: can't find remove browser by native identifier %d", browser->GetIdentifier());
    return nullptr;
  }

  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  thrift_codegen::RObject resultHandler;
  myService->exec([&](RpcExecutor::Service s){
    s->SchemeHandlerFactory_CreateHandler(resultHandler, myPeerId, bid, frm.get()->serverIdWithMap(), scheme_name.ToString(), req.get()->serverIdWithMap());
  });
  return resultHandler.objId != -1 ? new RemoteResourceHandler(bid, myService, resultHandler) : nullptr;
}