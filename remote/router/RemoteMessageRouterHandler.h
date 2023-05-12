#ifndef JCEF_REMOTEMESSAGEROUTERHANDLER_H
#define JCEF_REMOTEMESSAGEROUTERHANDLER_H

#include "../RemoteObjectFactory.h"
#include "../gen-cpp/shared_types.h"
#include "include/wrapper/cef_message_router.h"

class RemoteClientHandler;
class ClientsManager;

// Created in MessageRouter_AddHandler, disposed in MessageRouter_RemoveHandler.
// TODO: add simple leak protection (link with owner RemoteMessageRouter)
class RemoteMessageRouterHandler  : public CefMessageRouterBrowserSide::Handler, public RemoteObjectBase<RemoteMessageRouterHandler> {
 public:
  // Use shared_ptr because need to share pointer between threads
  static RemoteMessageRouterHandler* create(std::shared_ptr<RpcExecutor> service, std::shared_ptr<ClientsManager> manager, thrift_codegen::RObject peer);

  virtual bool OnQuery(CefRefPtr<CefBrowser> browser,
                       CefRefPtr<CefFrame> frame,
                       int64 query_id,
                       const CefString& request,
                       bool persistent,
                       CefRefPtr<Callback> callback) override;
  virtual void OnQueryCanceled(CefRefPtr<CefBrowser> browser,
                               CefRefPtr<CefFrame> frame,
                               int64 query_id) override;

 private:
  std::shared_ptr<ClientsManager> myClientsManager; // needed to obtain bid by CefRefPtr<CefBrowser>

  explicit RemoteMessageRouterHandler(std::shared_ptr<RpcExecutor> service, std::shared_ptr<ClientsManager> manager, int id, int peerId);
};

#endif  // JCEF_REMOTEMESSAGEROUTERHANDLER_H
