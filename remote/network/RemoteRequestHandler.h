#ifndef JCEF_REMOTEREQUESTHANDLER_H
#define JCEF_REMOTEREQUESTHANDLER_H

#include <set>

#include "include/cef_request_handler.h"

class ServerHandlerContext;

class RemoteRequestHandler : public CefRequestHandler {
 public:
  explicit RemoteRequestHandler(
      int bid,
      std::shared_ptr<ServerHandlerContext> ctx);
  virtual ~RemoteRequestHandler();

  // Called on the UI thread before browser navigation.
  bool OnBeforeBrowse(CefRefPtr<CefBrowser> browser,
                      CefRefPtr<CefFrame> frame,
                      CefRefPtr<CefRequest> request,
                      bool user_gesture,
                      bool is_redirect) override;

  // Called on the UI thread before OnBeforeBrowse in certain limited cases
  // where navigating a new or different browser might be desirable.
  bool OnOpenURLFromTab(CefRefPtr<CefBrowser> browser,
                        CefRefPtr<CefFrame> frame,
                        const CefString& target_url,
                        WindowOpenDisposition target_disposition,
                        bool user_gesture) override;

  // Called on the browser process IO thread before a resource request is
  // initiated.
  CefRefPtr<CefResourceRequestHandler> GetResourceRequestHandler(
      CefRefPtr<CefBrowser> browser,
      CefRefPtr<CefFrame> frame,
      CefRefPtr<CefRequest> request,
      bool is_navigation,
      bool is_download,
      const CefString& request_initiator,
      bool& disable_default_handling) override;

  // Called on the IO thread when the browser needs credentials from the user.
  // |origin_url| is the origin making this authentication request.
  bool GetAuthCredentials(CefRefPtr<CefBrowser> browser,
                          const CefString& origin_url,
                          bool isProxy,
                          const CefString& host,
                          int port,
                          const CefString& realm,
                          const CefString& scheme,
                          CefRefPtr<CefAuthCallback> callback) override;

  // Called on the UI thread to handle requests for URLs with an invalid
  // SSL certificate.
  bool OnCertificateError(CefRefPtr<CefBrowser> browser,
                          cef_errorcode_t cert_error,
                          const CefString& request_url,
                          CefRefPtr<CefSSLInfo> ssl_info,
                          CefRefPtr<CefCallback> callback) override;

  // Called on the browser process UI thread when the render process
  // terminates unexpectedly.
  void OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser,
                                 TerminationStatus status) override;

 private:
  const int myBid;
  std::shared_ptr<ServerHandlerContext> myCtx;

  std::set<int> myCallbacks;
  std::set<int> myAuthCallbacks;

  IMPLEMENT_REFCOUNTING(RemoteRequestHandler);
};

#endif  // JCEF_REMOTEREQUESTHANDLER_H
