#include "RemoteResourceRequestHandler.h"
#include "../CefUtils.h"
#include "../handlers/RemoteClientHandler.h"
#include "../log/Log.h"
#include "RemoteCookieAccessFilter.h"
#include "RemoteRequest.h"
#include "RemoteResourceHandler.h"
#include "RemoteResponse.h"

namespace {
  std::string status2str(cef_urlrequest_status_t type);
}

// Disable logging until optimized
#define LNDCT()

RemoteResourceRequestHandler::RemoteResourceRequestHandler(
    int bid,
    std::shared_ptr<RpcExecutor> serviceIO,
    thrift_codegen::RObject peer)
    : RemoteJavaObject(
          serviceIO,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->ResourceRequestHandler_Dispose(peer.objId);
          }), myBid(bid) {}

CefRefPtr<CefCookieAccessFilter>
RemoteResourceRequestHandler::GetCookieAccessFilter(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request
) {
  LNDCT();

  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  thrift_codegen::RObject remoteHandler;
  
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_GetCookieAccessFilter(remoteHandler, myPeerId, myBid, rr->serverIdWithMap());
  });
  return remoteHandler.objId != -1 ? new RemoteCookieAccessFilter(myBid, myService, remoteHandler) : nullptr;
}

CefResourceRequestHandler::ReturnValue
RemoteResourceRequestHandler::OnBeforeResourceLoad(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefCallback> callback
) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  CefResourceRequestHandler::ReturnValue result = RV_CONTINUE;
  myService->exec([&](RpcExecutor::Service s){
    bool boolRes = s->ResourceRequestHandler_OnBeforeResourceLoad(myPeerId, myBid, rr->serverIdWithMap());
    result = (boolRes ? RV_CANCEL : RV_CONTINUE);
  });
  return result;
}

CefRefPtr<CefResourceHandler> RemoteResourceRequestHandler::GetResourceHandler(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request
) {
  LNDCT();

  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  thrift_codegen::RObject remoteHandler;
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_GetResourceHandler(remoteHandler, myPeerId, myBid, rr->serverIdWithMap());
  });
  return remoteHandler.objId != -1 ? new RemoteResourceHandler(myBid, myService, remoteHandler) : nullptr;
}

void RemoteResourceRequestHandler::OnResourceRedirect(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response,
    CefString& new_url
) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(response);
  Holder<RemoteResponse> holderResp(*rresp);
  std::string result;
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_OnResourceRedirect(result, myPeerId, myBid, rr->serverIdWithMap(),
                                                 rresp->serverIdWithMap(), new_url.ToString());
  });
  CefString tmp(result);
  new_url.swap(tmp);
}

bool RemoteResourceRequestHandler::OnResourceResponse(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response
) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(response);
  Holder<RemoteResponse> holderResp(*rresp);
  return myService->exec<bool>([&](RpcExecutor::Service s){
    return s->ResourceRequestHandler_OnResourceResponse(myPeerId, myBid, rr->serverIdWithMap(),
                                                          rresp->serverIdWithMap());
  }, false);
}

void RemoteResourceRequestHandler::OnResourceLoadComplete(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response,
    CefResourceRequestHandler::URLRequestStatus status,
    int64_t received_content_length
) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(response);
  Holder<RemoteResponse> holderResp(*rresp);
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_OnResourceLoadComplete(myPeerId, myBid, rr->serverIdWithMap(),
                                                     rresp->serverIdWithMap(), status2str(status), received_content_length);
  });
}

void RemoteResourceRequestHandler::OnProtocolExecution(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    bool& allow_os_execution) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  myService->exec([&](RpcExecutor::Service s){
    allow_os_execution = s->ResourceRequestHandler_OnProtocolExecution(myPeerId, myBid, rr->serverIdWithMap(), allow_os_execution);
  });
}

namespace {
  std::pair<cef_urlrequest_status_t, std::string> statuses[] = {
      {UR_UNKNOWN, "UR_UNKNOWN"},
      {UR_SUCCESS, "UR_SUCCESS"},
      {UR_IO_PENDING, "UR_IO_PENDING"},
      {UR_CANCELED, "UR_CANCELED"},
      {UR_FAILED, "UR_FAILED"}
  };

  std::string status2str(cef_urlrequest_status_t type) {
    for (auto p : statuses) {
      if (p.first == type)
        return p.second;
    }
    return string_format("unknown_urlrequest_status_%d", type);
  }
}
