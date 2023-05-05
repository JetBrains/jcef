#ifndef JCEF_REMOTELOADHANDLER_H
#define JCEF_REMOTELOADHANDLER_H

#include <thrift/Thrift.h>
#include "include/cef_load_handler.h"

class RemoteClientHandler;
class RemoteLoadHandler : public CefLoadHandler {
 public:
  RemoteLoadHandler(RemoteClientHandler & owner);

  void OnLoadingStateChange(CefRefPtr<CefBrowser> browser,
                            bool isLoading,
                            bool canGoBack,
                            bool canGoForward) override;

  void OnLoadStart(CefRefPtr<CefBrowser> browser,
                   CefRefPtr<CefFrame> frame,
                   TransitionType transition_type) override;

  void OnLoadEnd(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 int httpStatusCode) override;

  void OnLoadError(CefRefPtr<CefBrowser> browser,
                   CefRefPtr<CefFrame> frame,
                   ErrorCode errorCode,
                   const CefString& errorText,
                   const CefString& failedUrl) override;

 protected:
  RemoteClientHandler & myOwner;

 private:
  IMPLEMENT_REFCOUNTING(RemoteLoadHandler);
};

#endif  // JCEF_REMOTELOADHANDLER_H
