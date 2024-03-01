#include "RemoteCookieAccessFilter.h"
#include "../handlers/RemoteClientHandler.h"
#include "../log/Log.h"
#include "RemoteRequest.h"
#include "RemoteResponse.h"
#include "../browser/RemoteFrame.h"

namespace {
  std::vector<std::string> cookie2list(const CefCookie& cookie);
}

// Disable logging until optimized
#ifdef LNDCT
#undef LNDCT
#define LNDCT()
#endif

RemoteCookieAccessFilter::RemoteCookieAccessFilter(
    int bid,
    std::shared_ptr<RpcExecutor> service,
    thrift_codegen::RObject peer)
    : RemoteJavaObject<RemoteCookieAccessFilter>(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->CookieAccessFilter_Dispose(peer.objId);
          }), myBid(bid) {}

///
/// Called on the IO thread before a resource request is sent. The |browser|
/// and |frame| values represent the source of the request, and may be NULL
/// for requests originating from service workers or CefURLRequest. |request|
/// cannot be modified in this callback. Return true if the specified cookie
/// can be sent with the request or false otherwise.
///
/*--cef(optional_param=browser,optional_param=frame)--*/
bool RemoteCookieAccessFilter::CanSendCookie(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefRefPtr<CefRequest> request,
                                             const CefCookie& cookie
) {
  LNDCT();
  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  return myService->exec<bool>([&](RpcExecutor::Service s){
    return s->CookieAccessFilter_CanSendCookie(myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(), cookie2list(cookie));
  }, true);
}

///
/// Called on the IO thread after a resource response is received. The
/// |browser| and |frame| values represent the source of the request, and may
/// be NULL for requests originating from service workers or CefURLRequest.
/// |request| cannot be modified in this callback. Return true if the
/// specified cookie returned with the response can be saved or false
/// otherwise.
///
/*--cef(optional_param=browser,optional_param=frame)--*/
bool RemoteCookieAccessFilter::CanSaveCookie(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefRefPtr<CefRequest> request,
                                             CefRefPtr<CefResponse> response,
                                             const CefCookie& cookie
) {
  LNDCT();
  RemoteRequest::Holder req(request);
  RemoteResponse::Holder resp(response);
  RemoteFrame::Holder frm(frame);
  return myService->exec<bool>([&](RpcExecutor::Service s){
    return s->CookieAccessFilter_CanSaveCookie(myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(),
                                               resp.get()->serverIdWithMap(), cookie2list(cookie));
  }, true);
}

namespace {
  std::string cefstr2std(cef_string_t cefstr) {
    CefString tmp(cefstr.str, cefstr.length);
    return tmp.ToString();
  }

  std::vector<std::string> cookie2list(const CefCookie& cookie) {
    std::vector<std::string> result;
    result.push_back(cefstr2std(cookie.name));
    result.push_back(cefstr2std(cookie.value));
    result.push_back(cefstr2std(cookie.domain));
    result.push_back(cefstr2std(cookie.path));
    result.push_back(std::to_string(cookie.secure));
    result.push_back(std::to_string(cookie.httponly));
    result.push_back(std::to_string(cookie.creation.val));
    result.push_back(std::to_string(cookie.last_access.val));
    result.push_back(std::to_string(cookie.has_expires));
    result.push_back(std::to_string(cookie.expires.val));
    return result;
  }
}
