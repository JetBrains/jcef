#ifndef JCEF_REMOTEPDFPRINTCALLBACK_H
#define JCEF_REMOTEPDFPRINTCALLBACK_H

#include "../RemoteObjects.h"
#include "include/cef_browser.h"

class RemotePdfPrintCallback : public CefPdfPrintCallback, public RemoteJavaObject<RemotePdfPrintCallback> {
 public:
  explicit RemotePdfPrintCallback(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer);
  virtual void OnPdfPrintFinished(const CefString& path, bool ok) override;

 private:
  IMPLEMENT_REFCOUNTING(RemotePdfPrintCallback);
};

#endif  // JCEF_REMOTEPDFPRINTCALLBACK_H
