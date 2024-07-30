#ifndef JCEF_REMOTEDEVTOOLSMESSAGEOBSERVER_H
#define JCEF_REMOTEDEVTOOLSMESSAGEOBSERVER_H

#include "include/cef_devtools_message_observer.h"
#include "../RemoteObjects.h"

class ClientsManager;

class RemoteDevToolsMessageObserver  : public CefDevToolsMessageObserver, public RemoteJavaObject<RemoteDevToolsMessageObserver>  {
 public:
  RemoteDevToolsMessageObserver(std::shared_ptr<ClientsManager> clientsManager, std::shared_ptr<RpcExecutor> service, thrift_codegen::RObject peer);

  void OnDevToolsEvent(CefRefPtr<CefBrowser> browser,
                       const CefString& method,
                       const void* params,
                       size_t params_size) override;

  void OnDevToolsMethodResult(CefRefPtr<CefBrowser> browser,
                              int message_id,
                              bool success,
                              const void* result,
                              size_t result_size) override;

 protected:
  std::shared_ptr<ClientsManager> myClientsManager;

  IMPLEMENT_REFCOUNTING(RemoteDevToolsMessageObserver);
};

#endif  // JCEF_REMOTEDEVTOOLSMESSAGEOBSERVER_H
