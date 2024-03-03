#include "RemoteRequestContextHandler.h"
#include "RemoteRequest.h"
#include "RemoteResourceRequestHandler.h"
#include "../ServerHandlerContext.h"
#include "../browser/RemoteFrame.h"
#include "../browser/ClientsManager.h"

RemoteRequestContextHandler::RemoteRequestContextHandler(std::shared_ptr<ServerHandlerContext> ctx, thrift_codegen::RObject peer) :
      RemoteJavaObject<RemoteRequestContextHandler>(
            ctx->javaService(),
            peer.objId,
            [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
              // Nothing to do, because lifetime of java-peer is managed by java owner (RemoteRequestContext)
            }), myCtx(ctx) {}

CefRefPtr<CefResourceRequestHandler> RemoteRequestContextHandler::GetResourceRequestHandler(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    bool is_navigation,
    bool is_download,
    const CefString& request_initiator,
    bool& disable_default_handling
) {
  // Called on the browser process IO thread before a resource request is initiated.
  LogNdc ndc(__FILE_NAME__, __FUNCTION__, 500, false, false, "ChromeIO");

  int bid = myCtx->clientsManager()->findRemoteBrowser(browser);

  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  thrift_codegen::RObject peer;
  peer.__set_objId(-1);
  myService->exec([&](RpcExecutor::Service s){
    s->RequestContextHandler_GetResourceRequestHandler(
        peer, myPeerId, bid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(), is_navigation, is_download, request_initiator.ToString());
  });

  disable_default_handling = peer.__isset.flags ? peer.flags != 0 : false;
  return peer.objId != -1 ? new RemoteResourceRequestHandler(bid, myCtx->javaServiceIO(), peer) : nullptr;
}
