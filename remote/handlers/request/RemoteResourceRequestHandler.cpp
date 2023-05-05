#include "../../log/Log.h"
#include "../../CefUtils.h"
#include "../RemoteClientHandler.h"
#include "RemoteResourceRequestHandler.h"
#include "RemoteResourceHandler.h"
#include "RemoteRequest.h"
#include "RemoteResponse.h"
#include "RemoteCookieAccessFilter.h"

namespace {
  std::string status2str(cef_urlrequest_status_t type);
}

RemoteResourceRequestHandler::RemoteResourceRequestHandler(RemoteClientHandler& owner, int id, int peerId)
    : RemoteObject(owner, id, peerId, [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) { service->ResourceRequestHandler_Dispose(peerId); }) {}

CefRefPtr<RemoteResourceRequestHandler> RemoteResourceRequestHandler::create(RemoteClientHandler& owner, int peerId) {
  return FACTORY.create([&](int id) -> RemoteResourceRequestHandler* {return new RemoteResourceRequestHandler(owner, id, peerId);});
}

CefRefPtr<CefCookieAccessFilter>
RemoteResourceRequestHandler::GetCookieAccessFilter(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request
) {
  LogNdc ndc("RemoteResourceRequestHandler::GetCookieAccessFilter");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return nullptr;

  if (myCookieAccessFilterReceived)
    return myCookieAccessFilter;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  thrift_codegen::RObject remoteHandler;
  try {
    remoteService->ResourceRequestHandler_GetCookieAccessFilter(remoteHandler, myPeerId, myOwner.getBid(), rr->toThriftWithMap());
    myCookieAccessFilterReceived = true;
    if (!remoteHandler.__isset.isPersistent || !remoteHandler.isPersistent)
      Log::error("Non-persistent CookieAccessFilter can cause unstable behaviour and won't be used.");
    else if (remoteHandler.objId != -1) {
        myCookieAccessFilter = RemoteCookieAccessFilter::create(myOwner, remoteHandler.objId); // returns ref-ptr (disposes java-object in dtor)
    }
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return myCookieAccessFilter;
}

CefResourceRequestHandler::ReturnValue
RemoteResourceRequestHandler::OnBeforeResourceLoad(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefCallback> callback) {
  LogNdc ndc("RemoteResourceRequestHandler::OnBeforeResourceLoad");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return RV_CONTINUE;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  try {
    bool result = remoteService->ResourceRequestHandler_OnBeforeResourceLoad(myPeerId, myOwner.getBid(), rr->toThriftWithMap());
    return (result ? RV_CANCEL : RV_CONTINUE);
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return RV_CONTINUE;
}

CefRefPtr<CefResourceHandler> RemoteResourceRequestHandler::GetResourceHandler(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request) {
  LogNdc ndc("RemoteResourceRequestHandler::GetResourceHandler");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return nullptr;

  if (myResourceHandlerReceived)
    return myResourceHandler;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  try {
    thrift_codegen::RObject remoteHandler;
    remoteService->ResourceRequestHandler_GetResourceHandler(remoteHandler, myPeerId, myOwner.getBid(), rr->toThriftWithMap());
    myResourceHandlerReceived = true;
    if (!remoteHandler.__isset.isPersistent || !remoteHandler.isPersistent)
        Log::error("Non-persistent ResourceHandler can cause unstable behaviour and won't be used.");
    else if (remoteHandler.objId != -1) {
        myResourceHandler = RemoteResourceHandler::create(myOwner, remoteHandler.objId); // returns ref-ptr (disposes java-object in dtor)
    }
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return myResourceHandler;
}

void RemoteResourceRequestHandler::OnResourceRedirect(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response,
    CefString& new_url) {
  LogNdc ndc("RemoteResourceRequestHandler::OnResourceRedirect");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(myOwner, response);
  Holder<RemoteResponse> holderResp(*rresp);
  try {
    std::string result;
    remoteService->ResourceRequestHandler_OnResourceRedirect(result, myPeerId, myOwner.getBid(), rr->toThriftWithMap(),
        rresp->toThriftWithMap(), new_url.ToString());
    CefString tmp(result);
    new_url.swap(tmp);
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

bool RemoteResourceRequestHandler::OnResourceResponse(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response) {
  LogNdc ndc("RemoteResourceRequestHandler::OnResourceResponse");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(myOwner, response);
  Holder<RemoteResponse> holderResp(*rresp);
  try {
    return remoteService->ResourceRequestHandler_OnResourceResponse(myPeerId, myOwner.getBid(), rr->toThriftWithMap(),
        rresp->toThriftWithMap());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return false;
}

void RemoteResourceRequestHandler::OnResourceLoadComplete(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response,
    CefResourceRequestHandler::URLRequestStatus status,
    int64 received_content_length) {
  LogNdc ndc("RemoteResourceRequestHandler::OnResourceLoadComplete");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  RemoteResponse * rresp = RemoteResponse::create(myOwner, response);
  Holder<RemoteResponse> holderResp(*rresp);
  try {
    remoteService->ResourceRequestHandler_OnResourceLoadComplete(myPeerId, myOwner.getBid(), rr->toThriftWithMap(),
        rresp->toThriftWithMap(), status2str(status), received_content_length);
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

void RemoteResourceRequestHandler::OnProtocolExecution(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    bool& allow_os_execution) {
  LogNdc ndc("RemoteResourceRequestHandler::OnProtocolExecution");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  try {
    bool result = remoteService->ResourceRequestHandler_OnProtocolExecution(myPeerId, myOwner.getBid(), rr->toThriftWithMap(), allow_os_execution);
    allow_os_execution = result;
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
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
