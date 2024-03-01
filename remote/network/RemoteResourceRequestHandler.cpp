#include "RemoteResourceRequestHandler.h"
#include "../CefUtils.h"
#include "../handlers/RemoteClientHandler.h"
#include "../log/Log.h"
#include "RemoteCookieAccessFilter.h"
#include "RemoteRequest.h"
#include "RemoteResourceHandler.h"
#include "RemoteResponse.h"
#include "../browser/RemoteFrame.h"

namespace {
  std::string status2str(cef_urlrequest_status_t type);
}

// Disable logging until optimized
#ifdef LNDCT
#undef LNDCT
#define LNDCT()
#endif

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

///
/// Called on the IO thread before a resource request is loaded. The |browser|
/// and |frame| values represent the source of the request, and may be NULL
/// for requests originating from service workers or CefURLRequest. To
/// optionally filter cookies for the request return a CefCookieAccessFilter
/// object. The |request| object cannot not be modified in this callback.
///
/*--cef(optional_param=browser,optional_param=frame)--*/
CefRefPtr<CefCookieAccessFilter> RemoteResourceRequestHandler::GetCookieAccessFilter(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request
) {
  LNDCT();

  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  thrift_codegen::RObject remoteHandler;
  
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_GetCookieAccessFilter(remoteHandler, myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap());
  });
  return remoteHandler.objId != -1 ? new RemoteCookieAccessFilter(myBid, myService, remoteHandler) : nullptr;
}

///
/// Called on the IO thread before a resource request is loaded. The |browser|
/// and |frame| values represent the source of the request, and may be NULL
/// for requests originating from service workers or CefURLRequest. To
/// redirect or change the resource load optionally modify |request|.
/// Modification of the request URL will be treated as a redirect. Return
/// RV_CONTINUE to continue the request immediately. Return RV_CONTINUE_ASYNC
/// and call CefCallback methods at a later time to continue or cancel the
/// request asynchronously. Return RV_CANCEL to cancel the request
/// immediately.
///
/*--cef(optional_param=browser,optional_param=frame, default_retval=RV_CONTINUE)--*/
CefResourceRequestHandler::ReturnValue RemoteResourceRequestHandler::OnBeforeResourceLoad(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefCallback> callback
) {
  LNDCT();
  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  CefResourceRequestHandler::ReturnValue result = RV_CONTINUE;
  myService->exec([&](RpcExecutor::Service s){
    bool boolRes = s->ResourceRequestHandler_OnBeforeResourceLoad(myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap());
    result = (boolRes ? RV_CANCEL : RV_CONTINUE);
  });
  return result;
}

///
/// Called on the IO thread before a resource is loaded. The |browser| and
/// |frame| values represent the source of the request, and may be NULL for
/// requests originating from service workers or CefURLRequest. To allow the
/// resource to load using the default network loader return NULL. To specify
/// a handler for the resource return a CefResourceHandler object. The
/// |request| object cannot not be modified in this callback.
///
/*--cef(optional_param=browser,optional_param=frame)--*/
CefRefPtr<CefResourceHandler> RemoteResourceRequestHandler::GetResourceHandler(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request
) {
  LNDCT();

  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  thrift_codegen::RObject remoteHandler;
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_GetResourceHandler(remoteHandler, myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap());
  });
  return remoteHandler.objId != -1 ? new RemoteResourceHandler(myBid, myService, remoteHandler) : nullptr;
}

///
/// Called on the IO thread when a resource load is redirected. The |browser|
/// and |frame| values represent the source of the request, and may be NULL
/// for requests originating from service workers or CefURLRequest. The
/// |request| parameter will contain the old URL and other request-related
/// information. The |response| parameter will contain the response that
/// resulted in the redirect. The |new_url| parameter will contain the new URL
/// and can be changed if desired. The |request| and |response| objects cannot
/// be modified in this callback.
///
/*--cef(optional_param=browser,optional_param=frame)--*/
void RemoteResourceRequestHandler::OnResourceRedirect(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response,
    CefString& new_url
) {
  LNDCT();
  RemoteRequest::Holder req(request);
  RemoteResponse::Holder resp(response);
  RemoteFrame::Holder frm(frame);
  std::string result;
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_OnResourceRedirect(result, myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(),
                                                 resp.get()->serverIdWithMap(), new_url.ToString());
  });
  CefString tmp(result);
  new_url.swap(tmp);
}

///
/// Called on the IO thread when a resource response is received. The
/// |browser| and |frame| values represent the source of the request, and may
/// be NULL for requests originating from service workers or CefURLRequest. To
/// allow the resource load to proceed without modification return false. To
/// redirect or retry the resource load optionally modify |request| and return
/// true. Modification of the request URL will be treated as a redirect.
/// Requests handled using the default network loader cannot be redirected in
/// this callback. The |response| object cannot be modified in this callback.
///
/// WARNING: Redirecting using this method is deprecated. Use
/// OnBeforeResourceLoad or GetResourceHandler to perform redirects.
///
/*--cef(optional_param=browser,optional_param=frame)--*/
bool RemoteResourceRequestHandler::OnResourceResponse(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response
) {
  LNDCT();
  RemoteRequest::Holder req(request);
  RemoteResponse::Holder resp(response);
  RemoteFrame::Holder frm(frame);
  return myService->exec<bool>([&](RpcExecutor::Service s){
    return s->ResourceRequestHandler_OnResourceResponse(myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(),
                                                        resp.get()->serverIdWithMap());
  }, false);
}

///
/// Called on the IO thread when a resource load has completed. The |browser|
/// and |frame| values represent the source of the request, and may be NULL
/// for requests originating from service workers or CefURLRequest. |request|
/// and |response| represent the request and response respectively and cannot
/// be modified in this callback. |status| indicates the load completion
/// status. |received_content_length| is the number of response bytes actually
/// read. This method will be called for all requests, including requests that
/// are aborted due to CEF shutdown or destruction of the associated browser.
/// In cases where the associated browser is destroyed this callback may
/// arrive after the CefLifeSpanHandler::OnBeforeClose callback for that
/// browser. The CefFrame::IsValid method can be used to test for this
/// situation, and care should be taken not to call |browser| or |frame|
/// methods that modify state (like LoadURL, SendProcessMessage, etc.) if the
/// frame is invalid.
///
/*--cef(optional_param=browser,optional_param=frame)--*/
void RemoteResourceRequestHandler::OnResourceLoadComplete(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    CefRefPtr<CefResponse> response,
    CefResourceRequestHandler::URLRequestStatus status,
    int64_t received_content_length
) {
  LNDCT();
  RemoteRequest::Holder req(request);
  RemoteResponse::Holder resp(response);
  RemoteFrame::Holder frm(frame);
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceRequestHandler_OnResourceLoadComplete(myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(),
                                                     resp.get()->serverIdWithMap(), status2str(status), received_content_length);
  });
}

///
/// Called on the IO thread to handle requests for URLs with an unknown
/// protocol component. The |browser| and |frame| values represent the source
/// of the request, and may be NULL for requests originating from service
/// workers or CefURLRequest. |request| cannot be modified in this callback.
/// Set |allow_os_execution| to true to attempt execution via the registered
/// OS protocol handler, if any. SECURITY WARNING: YOU SHOULD USE THIS METHOD
/// TO ENFORCE RESTRICTIONS BASED ON SCHEME, HOST OR OTHER URL ANALYSIS BEFORE
/// ALLOWING OS EXECUTION.
///
/*--cef(optional_param=browser,optional_param=frame)--*/
void RemoteResourceRequestHandler::OnProtocolExecution(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    bool& allow_os_execution) {
  LNDCT();
  RemoteRequest::Holder req(request);
  RemoteFrame::Holder frm(frame);
  myService->exec([&](RpcExecutor::Service s){
    allow_os_execution = s->ResourceRequestHandler_OnProtocolExecution(myPeerId, myBid, frm.get()->serverIdWithMap(), req.get()->serverIdWithMap(), allow_os_execution);
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
