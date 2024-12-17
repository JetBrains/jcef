#include "RemoteIntCallback.h"

RemoteIntCallback::RemoteIntCallback(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer) : RemoteJavaObject<RemoteIntCallback>(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            // Nothing to do (java-peer is disposed in the end of IntCallbackCallback_OnComplete)
          }) {}

void RemoteIntCallback::OnComplete(int result) {
  LNDCT();
  myCtx->javaService()->exec([&](RpcExecutor::Service s){
    s->IntCallback_OnComplete(myPeerId, result);
  });
}