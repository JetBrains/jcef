#include "RemoteCompletionCallback.h"

RemoteCompletionCallback::RemoteCompletionCallback(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer) : RemoteJavaObject<RemoteCompletionCallback>(
    service,
    peer.objId) {} // Empty disposer because java-peer is disposed in the end of CompletionCallback_OnComplete

void RemoteCompletionCallback::OnComplete() {
  myCtx->javaService()->exec([&](JavaService s){
    s->CompletionCallback_OnComplete(myPeerId);
  });
}