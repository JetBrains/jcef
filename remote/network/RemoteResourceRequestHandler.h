#ifndef JCEF_REMOTERESOURCEREQUESTHANDLER_H
#define JCEF_REMOTERESOURCEREQUESTHANDLER_H

#include "../RemoteObjects.h"
#include "RemoteCookieAccessFilter.h"
#include "include/cef_request_handler.h"

// All methods of this class will be called on the IO thread.
class RemoteResourceRequestHandler : public CefResourceRequestHandler, public RemoteJavaObject<RemoteResourceRequestHandler> {
 public:
  explicit RemoteResourceRequestHandler(int bid, std::shared_ptr<RpcExecutor> serviceIO, thrift_codegen::RObject peerd);

  // All methods of this 'sub-class' will be called on the IO thread.
  CefRefPtr<CefCookieAccessFilter> GetCookieAccessFilter(
      CefRefPtr<CefBrowser> browser,
      CefRefPtr<CefFrame> frame,
      CefRefPtr<CefRequest> request) override;

  ReturnValue OnBeforeResourceLoad(CefRefPtr<CefBrowser> browser,
                                   CefRefPtr<CefFrame> frame,
                                   CefRefPtr<CefRequest> request,
                                   CefRefPtr<CefCallback> callback) override;

  // All methods of this 'sub-class' will be called on the IO thread.
  CefRefPtr<CefResourceHandler> GetResourceHandler(
      CefRefPtr<CefBrowser> browser,
      CefRefPtr<CefFrame> frame,
      CefRefPtr<CefRequest> request) override;

  void OnResourceRedirect(CefRefPtr<CefBrowser> browser,
                          CefRefPtr<CefFrame> frame,
                          CefRefPtr<CefRequest> request,
                          CefRefPtr<CefResponse> response,
                          CefString& new_url) override;

  bool OnResourceResponse(CefRefPtr<CefBrowser> browser,
                          CefRefPtr<CefFrame> frame,
                          CefRefPtr<CefRequest> request,
                          CefRefPtr<CefResponse> response) override;

  // NOTE: use default (empty) GetResourceResponseFilter (as in JCEF impl)

  void OnResourceLoadComplete(CefRefPtr<CefBrowser> browser,
                              CefRefPtr<CefFrame> frame,
                              CefRefPtr<CefRequest> request,
                              CefRefPtr<CefResponse> response,
                              URLRequestStatus status,
                              int64_t received_content_length) override;

  void OnProtocolExecution(CefRefPtr<CefBrowser> browser,
                           CefRefPtr<CefFrame> frame,
                           CefRefPtr<CefRequest> request,
                           bool& allow_os_execution) override;

 private:
  const int myBid;

  IMPLEMENT_REFCOUNTING(RemoteResourceRequestHandler);
};

#endif  // JCEF_REMOTERESOURCEREQUESTHANDLER_H
