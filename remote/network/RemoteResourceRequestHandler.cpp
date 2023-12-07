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

RemoteResourceRequestHandler::RemoteResourceRequestHandler(RemoteClientHandler& owner, thrift_codegen::RObject peer)
    : RemoteJavaObject(owner, peer.objId, [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) { service->ResourceRequestHandler_Dispose(peer.objId); }) {}

CefRefPtr<CefCookieAccessFilter>
RemoteResourceRequestHandler::GetCookieAccessFilter(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request
) {
  LNDCT();
  if (myCookieAccessFilterReceived)
    return myCookieAccessFilter;

  RemoteRequest * rr = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holder(*rr);
  thrift_codegen::RObject remoteHandler;
  
  myOwner.exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_GetCookieAccessFilter(remoteHandler, myPeerId, myOwner.getBid(), rr->serverIdWithMap());
  });
  myCookieAccessFilterReceived = true;
  if (!remoteHandler.__isset.isPersistent || !remoteHandler.isPersistent)
    Log::error("Non-persistent CookieAccessFilter can cause unstable behaviour and won't be used.");
  else if (remoteHandler.objId != -1)
    myCookieAccessFilter = new RemoteCookieAccessFilter(myOwner, remoteHandler);

  return myCookieAccessFilter;
}

CefResourceRequestHandler::ReturnValue
RemoteResourceRequestHandler::OnBeforeResourceLoad(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefCallback> callback
) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holder(*rr);
  CefResourceRequestHandler::ReturnValue result = RV_CONTINUE;
  myOwner.exec([&](RpcExecutor::Service s){
    bool boolRes = s->ResourceRequestHandler_OnBeforeResourceLoad(myPeerId, myOwner.getBid(), rr->serverIdWithMap());
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
  if (myResourceHandlerReceived)
    return myResourceHandler;

  RemoteRequest * rr = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holder(*rr);
  thrift_codegen::RObject remoteHandler;
  myOwner.exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_GetResourceHandler(remoteHandler, myPeerId, myOwner.getBid(), rr->serverIdWithMap());
  });
  myResourceHandlerReceived = true;
  if (!remoteHandler.__isset.isPersistent || !remoteHandler.isPersistent)
    Log::error("Non-persistent ResourceHandler can cause unstable behaviour and won't be used.");
  else if (remoteHandler.objId != -1) {
    myResourceHandler = new RemoteResourceHandler(myOwner, remoteHandler);
  }
  return myResourceHandler;
}

void RemoteResourceRequestHandler::OnResourceRedirect(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response,
    CefString& new_url
) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(myOwner.getService(), response);
  Holder<RemoteResponse> holderResp(*rresp);
  std::string result;
  myOwner.exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_OnResourceRedirect(result, myPeerId, myOwner.getBid(), rr->serverIdWithMap(),
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
  RemoteRequest * rr = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(myOwner.getService(), response);
  Holder<RemoteResponse> holderResp(*rresp);
  return myOwner.exec<bool>([&](RpcExecutor::Service s){
    return s->ResourceRequestHandler_OnResourceResponse(myPeerId, myOwner.getBid(), rr->serverIdWithMap(),
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
  RemoteRequest * rr = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(myOwner.getService(), response);
  Holder<RemoteResponse> holderResp(*rresp);
  myOwner.exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_OnResourceLoadComplete(myPeerId, myOwner.getBid(), rr->serverIdWithMap(),
                                                     rresp->serverIdWithMap(), status2str(status), received_content_length);
  });
}

void RemoteResourceRequestHandler::OnProtocolExecution(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    bool& allow_os_execution) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(myOwner.getService(), request);
  Holder<RemoteRequest> holder(*rr);
  myOwner.exec([&](RpcExecutor::Service s){
    allow_os_execution = s->ResourceRequestHandler_OnProtocolExecution(myPeerId, myOwner.getBid(), rr->serverIdWithMap(), allow_os_execution);
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
