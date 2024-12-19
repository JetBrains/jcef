#include "RemoteIntCallback.h"

RemoteIntCallback::RemoteIntCallback(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer) : RemoteJavaObject<RemoteIntCallback>(
          service,
          peer.objId) {} // Empty disposer because java-peer is disposed in the end of IntCallbackCallback_OnComplete

void RemoteIntCallback::OnComplete(int result) {
  LNDCT();
  myCtx->javaService()->exec([&](JavaService s){
    s->IntCallback_OnComplete(myPeerId, result);
  });
}