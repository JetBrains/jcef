#include "RemoteCompletionCallback.h"

RemoteCompletionCallback::RemoteCompletionCallback(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer) : RemoteJavaObject<RemoteCompletionCallback>(
    service,
    peer.objId,
    [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
      // Nothing to do, java-peer is disposed in the end of CompletionCallback_OnComplete
    }) {}

void RemoteCompletionCallback::OnComplete() {
  LNDCT();
  myCtx->javaService()->exec([&](RpcExecutor::Service s){
    s->CompletionCallback_OnComplete(myPeerId);
  });
}