#ifndef JCEF_REMOTESCHEMEHANDLERFACTORY_H
#define JCEF_REMOTESCHEMEHANDLERFACTORY_H

#include "include/cef_scheme.h"
#include "../RemoteObjects.h"

class RemoteSchemeHandlerFactory : public CefSchemeHandlerFactory, public RemoteJavaObject<RemoteSchemeHandlerFactory>  {
 public:
  RemoteSchemeHandlerFactory(std::shared_ptr<ServerHandlerContext> ctx, thrift_codegen::RObject peer);

  CefRefPtr<CefResourceHandler> Create(CefRefPtr<CefBrowser> browser,
                                       CefRefPtr<CefFrame> frame,
                                       const CefString& scheme_name,
                                       CefRefPtr<CefRequest> request) override;

 protected:
  IMPLEMENT_REFCOUNTING(RemoteSchemeHandlerFactory);
};

#endif  // JCEF_REMOTESCHEMEHANDLERFACTORY_H
