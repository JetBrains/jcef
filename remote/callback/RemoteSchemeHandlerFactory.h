#ifndef JCEF_REMOTESCHEMEHANDLERFACTORY_H
#define JCEF_REMOTESCHEMEHANDLERFACTORY_H

#include "include/cef_scheme.h"
#include "../RemoteObjects.h"

class ClientsManager;

class RemoteSchemeHandlerFactory : public CefSchemeHandlerFactory, public RemoteJavaObject<RemoteSchemeHandlerFactory>  {
 public:
  RemoteSchemeHandlerFactory(std::shared_ptr<ClientsManager> clientsManager, std::shared_ptr<RpcExecutor> service, thrift_codegen::RObject peer);

  CefRefPtr<CefResourceHandler> Create(CefRefPtr<CefBrowser> browser,
                                       CefRefPtr<CefFrame> frame,
                                       const CefString& scheme_name,
                                       CefRefPtr<CefRequest> request) override;

 protected:
  std::shared_ptr<ClientsManager> myClientsManager;

  IMPLEMENT_REFCOUNTING(RemoteSchemeHandlerFactory);
};

#endif  // JCEF_REMOTESCHEMEHANDLERFACTORY_H
