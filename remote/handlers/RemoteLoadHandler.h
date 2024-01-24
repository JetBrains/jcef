#ifndef JCEF_REMOTELOADHANDLER_H
#define JCEF_REMOTELOADHANDLER_H

#include <thrift/Thrift.h>
#include "include/cef_load_handler.h"

class RemoteClientHandler;
class RpcExecutor;

class RemoteLoadHandler : public CefLoadHandler {
 public:
  explicit RemoteLoadHandler(int bid, std::shared_ptr<RpcExecutor> service);

  //
  // All next methods will be called on the UI thread
  //
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
  const int myBid;
  std::shared_ptr<RpcExecutor> myService;

 private:
  IMPLEMENT_REFCOUNTING(RemoteLoadHandler);
};

#endif  // JCEF_REMOTELOADHANDLER_H
