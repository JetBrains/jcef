#include "RemoteRequestHandler.h"
#include "../RpcExecutor.h"
#include "../callback/RemoteAuthCallback.h"
#include "../callback/RemoteCallback.h"
#include "RemoteRequest.h"
#include "RemoteResourceRequestHandler.h"
#include "../router/MessageRoutersManager.h"
#include "../browser/RemoteFrame.h"
#include "../browser/ClientsManager.h"
#include "../ServerHandlerContext.h"

#include "include/cef_ssl_info.h"

std::string err2str(cef_errorcode_t errorcode);
namespace {
  std::string tstatus2str(cef_termination_status_t status);
}

// Disable logging until optimized
#ifdef LNDCT
#undef LNDCT
#define LNDCT()
#endif

RemoteRequestHandler::RemoteRequestHandler(
    int bid,
    std::shared_ptr<ServerHandlerContext> ctx)
    : myBid(bid), myCtx(ctx) {}

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
  if (Log::isDebugEnabled()) {
    const int bid = myCtx->clientsManager()->findRemoteBrowser(browser);
    if (bid != myBid)
      Log::debug("RemoteRequestHandler::OnBeforeBrowse: bid mismatch, myBid(%d) != %d", myBid, bid);
  }
  // Forward request to ClientHandler to make the message_router_ happy.
  myCtx->routersManager()->OnBeforeBrowse(browser, frame);

  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  return myCtx->javaService()->exec<bool>([&](RpcExecutor::Service s){
    return s->RequestHandler_OnBeforeBrowse(myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(), user_gesture, is_redirect);
  }, false);
}

bool RemoteRequestHandler::OnOpenURLFromTab(CefRefPtr<CefBrowser> browser,
                      CefRefPtr<CefFrame> frame,
                      const CefString& target_url,
                      WindowOpenDisposition target_disposition,
                      bool user_gesture
) {
  LNDCT();
  if (Log::isDebugEnabled()) {
    const int bid = myCtx->clientsManager()->findRemoteBrowser(browser);
    if (bid != myBid)
      Log::debug("RemoteRequestHandler::OnOpenURLFromTab: bid mismatch, myBid(%d) != %d", myBid, bid);
  }
  RemoteFrame::Holder frm(frame);
  return myCtx->javaService()->exec<bool>([&](RpcExecutor::Service s){
    return s->RequestHandler_OnOpenURLFromTab(myBid, frm.get()->serverIdWithMap(), target_url.ToString(), user_gesture);
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
  if (Log::isDebugEnabled()) {
    const int bid = myCtx->clientsManager()->findRemoteBrowser(browser);
    if (bid != myBid)
      Log::debug("RemoteRequestHandler::GetResourceRequestHandler: bid mismatch, myBid(%d) != %d", myBid, bid);
  }

  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  thrift_codegen::RObject peer;
  peer.__set_objId(-1);
  myCtx->javaServiceIO()->exec([&](RpcExecutor::Service s){
    s->RequestHandler_GetResourceRequestHandler(
        peer, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(), is_navigation, is_download, request_initiator.ToString());
  });

  disable_default_handling = peer.__isset.flags ? peer.flags != 0 : false;
  return peer.objId != -1 ? new RemoteResourceRequestHandler(myBid, myCtx->javaServiceIO(), peer) : nullptr;
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
  if (Log::isDebugEnabled()) {
    const int bid = myCtx->clientsManager()->findRemoteBrowser(browser);
    if (bid != myBid)
      Log::debug("RemoteRequestHandler::GetAuthCredentials: bid mismatch, myBid(%d) != %d", myBid, bid);
  }
  thrift_codegen::RObject rc = RemoteAuthCallback::wrapDelegate(callback)->serverId();
  const bool handled = myCtx->javaServiceIO()->exec<bool>([&](RpcExecutor::Service s){
      return s->RequestHandler_GetAuthCredentials(myBid, origin_url.ToString(), isProxy, host.ToString(), port, realm.ToString(), scheme.ToString(), rc);
  }, false);
  if (!handled)
    RemoteAuthCallback::dispose(rc.objId);
  else
    myAuthCallbacks.insert(rc.objId); // Callback will be disposed with RemoteRequestHandler (just for insurance)
  return handled;
}

void writeSSLData(std::string & out, CefRefPtr<CefSSLInfo> sslInfo);

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
  if (Log::isDebugEnabled()) {
    const int bid = myCtx->clientsManager()->findRemoteBrowser(browser);
    if (bid != myBid)
      Log::debug("RemoteRequestHandler::OnCertificateError: bid mismatch, myBid(%d) != %d", myBid, bid);
  }
  RemoteCallback* rc = RemoteCallback::wrapDelegate(callback);
  std::string buf;
  writeSSLData(buf, ssl_info);
  if (buf.capacity() > 1024*128)
    Log::warn("Large SSL certificate data: %d bytes. Consider to use shared memory for IPC transport.", buf.capacity());
  const bool handled = myCtx->javaService()->exec<bool>([&](RpcExecutor::Service s){
      return s->RequestHandler_OnCertificateError(myBid, err2str(cert_error), request_url, buf, rc->serverId());
  }, false);
  if (!handled)
    RemoteCallback::dispose(rc->getId());
  else
    myCallbacks.insert(rc->getId()); // Callback will be disposed with RemoteRequestHandler (just for insurance)
  return handled;
}

size_t getAligned(size_t bytesCount) {
  const int diff = bytesCount % 4;
  return diff == 0 ? bytesCount : bytesCount + 4 - diff;
}

void writeSSLData(std::string & out, CefRefPtr<CefSSLInfo> sslInfo) {
  CefRefPtr<CefX509Certificate> cert = sslInfo->GetX509Certificate();

  // 1. Collect
  CefX509Certificate::IssuerChainBinaryList der_chain;
  cert->GetDEREncodedIssuerChain(der_chain);
  der_chain.insert(der_chain.begin(), cert->GetDEREncoded());

  // 2. Calculate size
  size_t totalBinaryDataSize = 0;
  for (const auto& it: der_chain) {
    const size_t size = it->GetSize();
    totalBinaryDataSize += getAligned(size);
  }

  // 3. Write
  const size_t reserved = totalBinaryDataSize + 4/*status mask*/ + 4/*items count*/ + 4*der_chain.size();
  out.resize(reserved);
  int32_t* p = (int32_t*)out.data();
  *(p++) = sslInfo->GetCertStatus();
  *(p++) = (int32_t)der_chain.size();
  //Log::trace("writeSSLData: chain size = %d, status = %d.", der_chain.size(), sslInfo->GetCertStatus());

  if (der_chain.size() == 0)
    return;

  for (const auto& der_cert: der_chain) {
    const size_t bytesCount = der_cert->GetSize();
    //Log::trace("writeSSLData: write chunk of size %d bytes, pos=%d.", bytesCount, ((char*)p) - out.data());
    *(p++) = (int32_t)bytesCount;
    if (bytesCount == 0)
      continue;

    der_cert->GetData(p, bytesCount, 0);
    p += getAligned(bytesCount)/4;
  }
}

void RemoteRequestHandler::OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser, TerminationStatus status) {
  LNDCT();
  if (Log::isDebugEnabled()) {
    const int bid = myCtx->clientsManager()->findRemoteBrowser(browser);
    if (bid != myBid)
      Log::debug("RemoteRequestHandler::OnRenderProcessTerminated: bid mismatch, myBid(%d) != %d", myBid, bid);
  }
  // Forward request to ClientHandler to make the message_router_ happy.
  myCtx->routersManager()->OnRenderProcessTerminated(browser);
  myCtx->javaService()->exec([&](RpcExecutor::Service s){
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
