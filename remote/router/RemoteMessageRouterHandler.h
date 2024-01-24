#ifndef JCEF_REMOTEMESSAGEROUTERHANDLER_H
#define JCEF_REMOTEMESSAGEROUTERHANDLER_H

#include "../RemoteObjects.h"
#include "../gen-cpp/shared_types.h"
#include "include/wrapper/cef_message_router.h"

class RemoteClientHandler;
class ClientsManager;

// Created in MessageRouter_AddHandler, disposed in MessageRouter_RemoveHandler.
// Owned (and managed) by RemoteMessageRouter
class RemoteMessageRouterHandler : public CefMessageRouterBrowserSide::Handler, public RemoteJavaObject<RemoteMessageRouterHandler> {
 public:
  // Use shared_ptr because need to share pointer between threads
  explicit RemoteMessageRouterHandler(std::shared_ptr<RpcExecutor> service, std::shared_ptr<ClientsManager> manager, thrift_codegen::RObject peer);
  ~RemoteMessageRouterHandler() override;

  // All methods will be executed on the browser process UI thread.
  virtual bool OnQuery(CefRefPtr<CefBrowser> browser,
                       CefRefPtr<CefFrame> frame,
                       int64_t query_id,
                       const CefString& request,
                       bool persistent,
                       CefRefPtr<Callback> callback) override;
  virtual void OnQueryCanceled(CefRefPtr<CefBrowser> browser,
                               CefRefPtr<CefFrame> frame,
                               int64_t query_id) override;

 private:
  std::set<int> myCallbacks;
  std::shared_ptr<ClientsManager> myClientsManager; // necessary for finding bid by CefRefPtr<CefBrowser>
};

#endif  // JCEF_REMOTEMESSAGEROUTERHANDLER_H
