#include "RemoteMessageRouterHandler.h"
#include "RemoteQueryCallback.h"
#include "../browser/ClientsManager.h"

// remove to enable tracing
#define TRACE()

RemoteMessageRouterHandler::RemoteMessageRouterHandler(
    std::shared_ptr<RpcExecutor> service,
    std::shared_ptr<ClientsManager> manager,
    int id,
    int peerId)
    : RemoteObjectBase(
          service,
          id,
          peerId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->ResourceRequestHandler_Dispose(peerId);
          }),
      myClientsManager(manager) {
  TRACE();
}

RemoteMessageRouterHandler* RemoteMessageRouterHandler::create(
    std::shared_ptr<RpcExecutor> service,
    std::shared_ptr<ClientsManager> manager,
    thrift_codegen::RObject peer) {
  return FACTORY.create([&](int id) -> RemoteMessageRouterHandler* {
    return new RemoteMessageRouterHandler(service, manager, id, peer.objId);
  });
}

bool RemoteMessageRouterHandler::OnQuery(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     int64 query_id,
                     const CefString& request,
                     bool persistent,
                     CefRefPtr<Callback> callback) {
  TRACE();
  const int bid = myClientsManager->findRemoteBrowser(browser);
  if (bid < 0) {
    Log::error("Can't find remote browser by cef-id %d", browser ? browser->GetIdentifier() : -1);
    return false;
  }
  RemoteQueryCallback * rcb = RemoteQueryCallback::create(myService, callback);
  const int rcdId = rcb->getId();
  bool handled = myService->exec<bool>([&](RpcExecutor::Service s){
    return s->MessageRouterHandler_onQuery(toThrift(), bid, query_id, request, persistent, rcb->toThrift());
  }, false);
  if (!handled) // NOTE: must delete callback when onQuery returns false
    RemoteQueryCallback::dispose(rcdId);
  return handled;
}

void RemoteMessageRouterHandler::OnQueryCanceled(CefRefPtr<CefBrowser> browser,
                             CefRefPtr<CefFrame> frame,
                             int64 query_id) {
  TRACE();
  const int bid = myClientsManager->findRemoteBrowser(browser);
  if (bid < 0) {
    Log::error("Can't find remote browser by cef-id %d", browser ? browser->GetIdentifier() : -1);
    return;
  }
  myService->exec([&](RpcExecutor::Service s){
    return s->MessageRouterHandler_onQueryCanceled(toThrift(), bid, query_id);
  });
}