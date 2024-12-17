#include "RemoteStringVisitor.h"

RemoteStringVisitor::RemoteStringVisitor(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer) : RemoteJavaObject<RemoteStringVisitor>(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->StringVisitor_Dispose(myPeerId);
          }) {}

void RemoteStringVisitor::Visit(const CefString& str){
  LNDCT();
  myCtx->javaService()->exec([&](RpcExecutor::Service s){
     s->StringVisitor_Visit(myPeerId, str);
  });
}
