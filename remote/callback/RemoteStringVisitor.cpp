#include "RemoteStringVisitor.h"

RemoteStringVisitor::RemoteStringVisitor(std::shared_ptr<RpcExecutor> service, thrift_codegen::RObject peer) : RemoteJavaObject<RemoteStringVisitor>(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->StringVisitor_Dispose(myPeerId);
          }) {}

void RemoteStringVisitor::Visit(const CefString& str){
  LNDCT();
  myService->exec([&](RpcExecutor::Service s){
     s->StringVisitor_Visit(myPeerId, str);
  });
}
