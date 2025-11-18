#include "RemoteRequestContextHandler.h"
#include "RemoteRequest.h"
#include "RemoteResourceRequestHandler.h"
#include "../ServerHandlerContext.h"
#include "../browser/RemoteFrame.h"
#include "../browser/RemoteBrowser.h"

const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemoteRequestContextHandler");

RemoteRequestContextHandler::RemoteRequestContextHandler(std::shared_ptr<ServerHandlerContext> ctx, thrift_codegen::RObject peer) :
      RemoteJavaObject<RemoteRequestContextHandler>(ctx, peer.objId), myCtx(ctx) { // Empty disposer because lifetime of java-peer is managed by java owner (RemoteRequestContext)
  TRACE();
}

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
  TRACE();
  LogNdc ndc(__FILE_NAME__, __FUNCTION__, 500, false, false, "ChromeIO");
  std::shared_ptr<RemoteBrowser> rb = RemoteBrowser::findByCefBrowser(browser);
  if (!rb)
    return nullptr;

  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  thrift_codegen::RObject peer;
  myCtx->javaService()->exec([&](JavaService s){
    s->RequestContextHandler_GetResourceRequestHandler(
        peer, myPeerId, rb->getBid(), frm.serverId(), req.serverId(), is_navigation, is_download, request_initiator.ToString());
  });

  disable_default_handling = peer.__isset.flags ? peer.flags != 0 : false;
  return !peer.isNull ? new RemoteResourceRequestHandler(rb->getBid(), myCtx, peer) : nullptr;
}
