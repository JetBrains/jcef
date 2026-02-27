#include "RemoteSchemeHandlerFactory.h"
#include "../network/RemoteResourceHandler.h"
#include "../network/RemoteRequest.h"
#include "../browser/RemoteBrowser.h"
#include "../browser/RemoteFrame.h"

RemoteSchemeHandlerFactory::RemoteSchemeHandlerFactory(
    std::shared_ptr<ServerHandlerContext> service,
    thrift_codegen::RObject peer)
    : RemoteJavaObject<RemoteSchemeHandlerFactory>(
          service,
          peer.uid,
          [=](JavaService service) {
            service->SchemeHandlerFactory_Dispose(peer.uid);
            Log::trace("Disposed SchemeHandlerFactory, peer-id=%d", peer.uid);
          }) {
  Log::trace("Created SchemeHandlerFactory, peer-id=%d", peer.uid);
}

CefRefPtr<CefResourceHandler> RemoteSchemeHandlerFactory::Create(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    const CefString& scheme_name,
    CefRefPtr<CefRequest> request
) {
  const auto bid = RemoteBrowser::findBidByCefBrowser(browser);
  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  thrift_codegen::RObject resultHandler;
  myCtx->javaService()->exec([&](JavaService s){
    s->SchemeHandlerFactory_CreateHandler(resultHandler, myPeerId, bid, frm.toRObject(), scheme_name.ToString(), req.toRObject());
  });
  return !resultHandler.isNull ? new RemoteResourceHandler(myCtx, resultHandler) : nullptr;
}