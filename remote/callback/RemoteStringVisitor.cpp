#include "RemoteStringVisitor.h"

RemoteStringVisitor::RemoteStringVisitor(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer) : RemoteJavaObject<RemoteStringVisitor>(
          service,
          peer.objId,
          [=](JavaService service) {
            service->StringVisitor_Dispose(myPeerId);
          }) {}

void RemoteStringVisitor::Visit(const CefString& str){
  myCtx->javaService()->exec([&](JavaService s){
     s->StringVisitor_Visit(myPeerId, str);
  });
}
