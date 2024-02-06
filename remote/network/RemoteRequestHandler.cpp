#include "RemoteRequestHandler.h"
#include "../CefUtils.h"
#include "../callback/RemoteAuthCallback.h"
#include "../callback/RemoteCallback.h"
#include "../handlers/RemoteClientHandler.h"
#include "RemoteRequest.h"
#include "RemoteResourceRequestHandler.h"

std::string err2str(cef_errorcode_t errorcode);
namespace {
  std::string tstatus2str(cef_termination_status_t status);
}

// Disable logging until optimized
#define LNDCT()

RemoteRequestHandler::RemoteRequestHandler(
    int bid,
    std::shared_ptr<RpcExecutor> service,
    std::shared_ptr<RpcExecutor> serviceIO,
    std::shared_ptr<MessageRoutersManager> routersManager)
    : myBid(bid), myService(service), myServiceIO(serviceIO), myRoutersManager(routersManager) {}

RemoteRequestHandler::~RemoteRequestHandler() {
  // simple protection for leaking via callbacks
  for (auto c: myCallbacks)
    RemoteCallback::dispose(c);
  for (auto c: myAuthCallbacks)
    RemoteAuthCallback::dispose(c);
}

///
/// Called on the UI thread before browser navigation. Return true to cancel
/// the navigation or false to allow the navigation to proceed. The |request|
/// object cannot be modified in this callback.
/// CefLoadHandler::OnLoadingStateChange will be called twice in all cases.
/// If the navigation is allowed CefLoadHandler::OnLoadStart and
/// CefLoadHandler::OnLoadEnd will be called. If the navigation is canceled
/// CefLoadHandler::OnLoadError will be called with an |errorCode| value of
/// ERR_ABORTED. The |user_gesture| value will be true if the browser
/// navigated via explicit user gesture (e.g. clicking a link) or false if it
/// navigated automatically (e.g. via the DomContentLoaded event).
///
/*--cef()--*/
bool RemoteRequestHandler::OnBeforeBrowse(CefRefPtr<CefBrowser> browser,
                    CefRefPtr<CefFrame> frame,
                    CefRefPtr<CefRequest> request,
                    bool user_gesture,
                    bool is_redirect
) {
  LNDCT();
  // Forward request to ClientHandler to make the message_router_ happy.
  myRoutersManager->OnBeforeBrowse(browser, frame);

  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  return myService->exec<bool>([&](RpcExecutor::Service s){
    return s->RequestHandler_OnBeforeBrowse(myBid, rr->serverIdWithMap(), user_gesture, is_redirect);
  }, false);
}

bool RemoteRequestHandler::OnOpenURLFromTab(CefRefPtr<CefBrowser> browser,
                      CefRefPtr<CefFrame> frame,
                      const CefString& target_url,
                      WindowOpenDisposition target_disposition,
                      bool user_gesture
) {
  LNDCT();
  return myService->exec<bool>([&](RpcExecutor::Service s){
    return s->RequestHandler_OnOpenURLFromTab(myBid, target_url.ToString(), user_gesture);
  }, false);
}

///
/// Called on the browser process IO thread before a resource request is
/// initiated. The |browser| and |frame| values represent the source of the
/// request. |request| represents the request contents and cannot be modified
/// in this callback. |is_navigation| will be true if the resource request is
/// a navigation. |is_download| will be true if the resource request is a
/// download. |request_initiator| is the origin (scheme + domain) of the page
/// that initiated the request. Set |disable_default_handling| to true to
/// disable default handling of the request, in which case it will need to be
/// handled via CefResourceRequestHandler::GetResourceHandler or it will be
/// canceled. To allow the resource load to proceed with default handling
/// return NULL. To specify a handler for the resource return a
/// CefResourceRequestHandler object. If this callback returns NULL the same
/// method will be called on the associated CefRequestContextHandler, if any.
///
CefRefPtr<CefResourceRequestHandler> RemoteRequestHandler::GetResourceRequestHandler(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    bool is_navigation,
    bool is_download,
    const CefString& request_initiator,
    bool& disable_default_handling
) {
  // Called on the browser process IO thread before a resource request is initiated.
  LogNdc ndc(__FILE_NAME__, __FUNCTION__, 500, false, false, "ChromeIO");

  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  thrift_codegen::RObject peer;
  peer.__set_objId(-1);
  myServiceIO->exec([&](RpcExecutor::Service s){
    s->RequestHandler_GetResourceRequestHandler(
        peer, myBid, rr->serverIdWithMap(), is_navigation, is_download, request_initiator.ToString());
  });

  disable_default_handling = peer.__isset.flags ? peer.flags != 0 : false;
  return peer.objId != -1 ? new RemoteResourceRequestHandler(myBid, myServiceIO, peer) : nullptr;
}

///
/// Called on the IO thread when the browser needs credentials from the user.
/// |origin_url| is the origin making this authentication request. |isProxy|
/// indicates whether the host is a proxy server. |host| contains the hostname
/// and |port| contains the port number. |realm| is the realm of the challenge
/// and may be empty. |scheme| is the authentication scheme used, such as
/// "basic" or "digest", and will be empty if the source of the request is an
/// FTP server. Return true to continue the request and call
/// CefAuthCallback::Continue() either in this method or at a later time when
/// the authentication information is available. Return false to cancel the
/// request immediately.
///
/*--cef(optional_param=realm,optional_param=scheme)--*/
bool RemoteRequestHandler::GetAuthCredentials(CefRefPtr<CefBrowser> browser,
                        const CefString& origin_url,
                        bool isProxy,
                        const CefString& host,
                        int port,
                        const CefString& realm,
                        const CefString& scheme,
                        CefRefPtr<CefAuthCallback> callback
) {
  LNDCT();
  thrift_codegen::RObject rc = RemoteAuthCallback::create(callback);
  const bool handled = myServiceIO->exec<bool>([&](RpcExecutor::Service s){
      return s->RequestHandler_GetAuthCredentials(myBid, origin_url.ToString(), isProxy, host.ToString(), port, realm.ToString(), scheme.ToString(), rc);
  }, false);
  if (!handled)
    RemoteAuthCallback::dispose(rc.objId);
  else
    myAuthCallbacks.insert(rc.objId); // Callback will be disposed with RemoteRequestHandler (just for insurance)
  return handled;
}

///
/// Called on the UI thread to handle requests for URLs with an invalid
/// SSL certificate. Return true and call CefCallback methods either in this
/// method or at a later time to continue or cancel the request. Return false
/// to cancel the request immediately. If
/// cef_settings_t.ignore_certificate_errors is set all invalid certificates
/// will be accepted without calling this method.
///
/*--cef()--*/
bool RemoteRequestHandler::OnCertificateError(CefRefPtr<CefBrowser> browser,
                        cef_errorcode_t cert_error,
                        const CefString& request_url,
                        CefRefPtr<CefSSLInfo> ssl_info,
                        CefRefPtr<CefCallback> callback
) {
  LNDCT();
  thrift_codegen::RObject rc = RemoteCallback::create(callback);
  thrift_codegen::RObject sslInfo;
  sslInfo.__set_objId(-1); // TODO: implement ssl_info
  const bool handled = myService->exec<bool>([&](RpcExecutor::Service s){
      return s->RequestHandler_OnCertificateError(myBid, err2str(cert_error), request_url, sslInfo, rc);
  }, false);
  if (!handled)
    RemoteCallback::dispose(rc.objId);
  else
    myCallbacks.insert(rc.objId); // Callback will be disposed with RemoteRequestHandler (just for insurance)
  return handled;
}

void RemoteRequestHandler::OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser, TerminationStatus status) {
  LNDCT();
  // Forward request to ClientHandler to make the message_router_ happy.
  myRoutersManager->OnRenderProcessTerminated(browser);
  myService->exec([&](RpcExecutor::Service s){
    s->RequestHandler_OnRenderProcessTerminated(myBid, tstatus2str(status));
  });
}

namespace {
  std::pair<cef_errorcode_t, std::string> errorcodes [] = {
      #define NET_ERROR(label, value) {ERR_##label,"ERR_"#label},
      #include "include/base/internal/cef_net_error_list.h"
      #undef NET_ERROR
      {ERR_NONE, "ERR_NONE"}
  };

  std::pair<cef_termination_status_t, std::string> terminationStatuses [] = {
      {TS_ABNORMAL_TERMINATION, "TS_ABNORMAL_TERMINATION"},
      {TS_PROCESS_WAS_KILLED, "TS_PROCESS_WAS_KILLED"},
      {TS_PROCESS_CRASHED, "TS_PROCESS_CRASHED"},
      {TS_PROCESS_OOM, "TS_PROCESS_OOM"}
  };

  std::string tstatus2str(cef_termination_status_t status) {
    for (auto p: terminationStatuses) {
      if (p.first == status)
        return p.second;
    }
    return string_format("unknown_termination_status_%d", status);
  }
}

std::string err2str(cef_errorcode_t errorcode) {
    for (auto p: errorcodes) {
      if (p.first == errorcode)
        return p.second;
    }
    Log::error("Can't find cef_errorcode_t: %d", errorcode);
    return string_format("unknown_errorcode_%d", errorcode);
}

cef_errorcode_t str2err(std::string err) {
    for (auto p: errorcodes) {
      if (p.second.compare(err) == 0)
        return p.first;
    }
    Log::error("Can't find cef_errorcode_t: %s", err.c_str());
    return ERR_NONE;
}
