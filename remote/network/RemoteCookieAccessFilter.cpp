#include "RemoteCookieAccessFilter.h"
#include "../handlers/RemoteClientHandler.h"
#include "../log/Log.h"
#include "RemoteRequest.h"
#include "RemoteResponse.h"

namespace {
  std::vector<std::string> cookie2list(const CefCookie& cookie);
}

// Disable logging until optimized
#define LNDCT()

RemoteCookieAccessFilter::RemoteCookieAccessFilter(RemoteClientHandler& owner, thrift_codegen::RObject peer)
    : RemoteJavaObject<RemoteCookieAccessFilter>(owner, peer.objId,
    [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) { service->CookieAccessFilter_Dispose(peer.objId); }) {}

bool RemoteCookieAccessFilter::CanSendCookie(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefRefPtr<CefRequest> request,
                                             const CefCookie& cookie
) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holder(*rr);
  return myOwner.exec<bool>([&](RpcExecutor::Service s){
    return s->CookieAccessFilter_CanSendCookie(myPeerId, myOwner.getBid(), rr->serverIdWithMap(), cookie2list(cookie));
  }, true);
}

bool RemoteCookieAccessFilter::CanSaveCookie(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefRefPtr<CefRequest> request,
                                             CefRefPtr<CefResponse> response,
                                             const CefCookie& cookie
) {
  LNDCT();
  RemoteRequest * rreq = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holderReq(*rreq);
  RemoteResponse * rresp = RemoteResponse::create(myOwner.getService(), response);
  Holder<RemoteResponse> holderResp(*rresp);
  return myOwner.exec<bool>([&](RpcExecutor::Service s){
    return s->CookieAccessFilter_CanSaveCookie(myPeerId, myOwner.getBid(), rreq->serverIdWithMap(),
                                                 rresp->serverIdWithMap(), cookie2list(cookie));
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
