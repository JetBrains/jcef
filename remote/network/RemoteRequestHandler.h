#ifndef JCEF_REMOTEREQUESTHANDLER_H
#define JCEF_REMOTEREQUESTHANDLER_H

#include "../Utils.h"
#include "include/cef_request_handler.h"

class RemoteClientHandler;
class MessageRoutersManager;

class RemoteRequestHandler : public CefRequestHandler {
 public:
  explicit RemoteRequestHandler(int bid, std::shared_ptr<RpcExecutor> service, std::shared_ptr<MessageRoutersManager> routersManager);
  virtual ~RemoteRequestHandler();

  bool OnBeforeBrowse(CefRefPtr<CefBrowser> browser,
                      CefRefPtr<CefFrame> frame,
                      CefRefPtr<CefRequest> request,
                      bool user_gesture,
                      bool is_redirect) override;
  bool OnOpenURLFromTab(CefRefPtr<CefBrowser> browser,
                        CefRefPtr<CefFrame> frame,
                        const CefString& target_url,
                        WindowOpenDisposition target_disposition,
                        bool user_gesture) override;
  CefRefPtr<CefResourceRequestHandler> GetResourceRequestHandler(
      CefRefPtr<CefBrowser> browser,
      CefRefPtr<CefFrame> frame,
      CefRefPtr<CefRequest> request,
      bool is_navigation,
      bool is_download,
      const CefString& request_initiator,
      bool& disable_default_handling) override;
  bool GetAuthCredentials(CefRefPtr<CefBrowser> browser,
                          const CefString& origin_url,
                          bool isProxy,
                          const CefString& host,
                          int port,
                          const CefString& realm,
                          const CefString& scheme,
                          CefRefPtr<CefAuthCallback> callback) override;
  bool OnCertificateError(CefRefPtr<CefBrowser> browser,
                          cef_errorcode_t cert_error,
                          const CefString& request_url,
                          CefRefPtr<CefSSLInfo> ssl_info,
                          CefRefPtr<CefCallback> callback) override;
  void OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser,
                                 TerminationStatus status) override;

 private:
  const int myBid;
  std::shared_ptr<RpcExecutor> myService;
  std::shared_ptr<MessageRoutersManager> myRoutersManager;

  std::set<int> myCallbacks;
  std::set<int> myAuthCallbacks;

  // Persistent java handler
  bool myResourceRequestHandlerReceived = false;
  bool myDisableDefaultHandling = false;
  CefRefPtr<CefResourceRequestHandler> myResourceRequestHandler;

  IMPLEMENT_REFCOUNTING(RemoteRequestHandler);
};

#endif  // JCEF_REMOTEREQUESTHANDLER_H
