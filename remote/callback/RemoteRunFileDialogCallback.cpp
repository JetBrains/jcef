#include "RemoteRunFileDialogCallback.h"

RemoteRunFileDialogCallback::RemoteRunFileDialogCallback(
    std::shared_ptr<ServerHandlerContext> service,
    thrift_codegen::RObject peer)
    : RemoteJavaObject<RemoteRunFileDialogCallback>(service, peer.uid) {
}  // Empty disposer because java-peer is disposed in the end of RunFileDialogCallback_OnFileDialogDismissed

void RemoteRunFileDialogCallback::OnFileDialogDismissed(const std::vector<CefString>& file_paths) {
  std::vector<std::string> filePaths;
  for (const auto & fp: file_paths)
    filePaths.push_back(fp.ToString());

  myCtx->javaService()->exec([&](JavaService s){
    s->RunFileDialogCallback_OnFileDialogDismissed(myPeerId, filePaths);
  });
}