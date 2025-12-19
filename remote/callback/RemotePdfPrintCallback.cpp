#include "RemotePdfPrintCallback.h"

RemotePdfPrintCallback::RemotePdfPrintCallback(
    std::shared_ptr<ServerHandlerContext> service,
    thrift_codegen::RObject peer)
    : RemoteJavaObject<RemotePdfPrintCallback>(service, peer.objId) {
}  // Empty disposer because java-peer is disposed in the end of RunFileDialogCallback_OnFileDialogDismissed

void RemotePdfPrintCallback::OnPdfPrintFinished(const CefString& path, bool ok) {
  myCtx->javaService()->exec([&](JavaService s){
    s->PdfPrintCallback_OnPdfPrintFinished(myPeerId, path.ToString(), ok);
  });
}
