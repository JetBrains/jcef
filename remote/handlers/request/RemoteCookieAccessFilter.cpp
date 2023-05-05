#include "../../log/Log.h"
#include "../RemoteClientHandler.h"
#include "RemoteRequest.h"
#include "RemoteResponse.h"
#include "RemoteCookieAccessFilter.h"

namespace {
  std::vector<std::string> cookie2list(const CefCookie& cookie);
}

RemoteCookieAccessFilter::RemoteCookieAccessFilter(RemoteClientHandler& owner, int id, int peerId)
    : RemoteObject<RemoteCookieAccessFilter>(owner, id, peerId,
    [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) { service->CookieAccessFilter_Dispose(peerId); }) {}

// TODO: use RObject everywhere in such create methods
CefRefPtr<RemoteCookieAccessFilter> RemoteCookieAccessFilter::create(RemoteClientHandler& owner, int peerId) {
  return FACTORY.create([&](int id) -> RemoteCookieAccessFilter* {return new RemoteCookieAccessFilter(owner, id, peerId);});
}

bool RemoteCookieAccessFilter::CanSendCookie(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefRefPtr<CefRequest> request,
                                             const CefCookie& cookie) {
  LogNdc ndc("RemoteCookieAccessFilter::CanSendCookie");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  try {
    return remoteService->CookieAccessFilter_CanSendCookie(myPeerId, myOwner.getBid(), rr->toThriftWithMap(), cookie2list(cookie));
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return true;
}

bool RemoteCookieAccessFilter::CanSaveCookie(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefRefPtr<CefRequest> request,
                                             CefRefPtr<CefResponse> response,
                                             const CefCookie& cookie) {
  LogNdc ndc("RemoteCookieAccessFilter::CanSaveCookie");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  RemoteRequest * rreq = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holderReq(*rreq);

  RemoteResponse * rresp = RemoteResponse::create(myOwner, response);
  Holder<RemoteResponse> holderResp(*rresp);
  try {
    return remoteService->CookieAccessFilter_CanSaveCookie(myPeerId, myOwner.getBid(), rreq->toThriftWithMap(),
        rresp->toThriftWithMap(), cookie2list(cookie));
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return true;
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
