#ifndef JCEF_REMOTERESOURCEREQUESTHANDLER_H
#define JCEF_REMOTERESOURCEREQUESTHANDLER_H

#include "../RemoteObjectFactory.h"
#include "include/cef_request_handler.h"
#include "RemoteCookieAccessFilter.h"

class RemoteResourceRequestHandler : public CefResourceRequestHandler, public RemoteObject<RemoteResourceRequestHandler> {
 public:
  static CefRefPtr<RemoteResourceRequestHandler> create(RemoteClientHandler & owner, int peerId);

  CefRefPtr<CefCookieAccessFilter> GetCookieAccessFilter(
      CefRefPtr<CefBrowser> browser,
      CefRefPtr<CefFrame> frame,
      CefRefPtr<CefRequest> request) override;

  ReturnValue OnBeforeResourceLoad(CefRefPtr<CefBrowser> browser,
                                   CefRefPtr<CefFrame> frame,
                                   CefRefPtr<CefRequest> request,
                                   CefRefPtr<CefCallback> callback) override;

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
                              int64 received_content_length) override;

  void OnProtocolExecution(CefRefPtr<CefBrowser> browser,
                           CefRefPtr<CefFrame> frame,
                           CefRefPtr<CefRequest> request,
                           bool& allow_os_execution) override;

 private:
  // Persistent java handlers
  bool myCookieAccessFilterReceived = false;
  bool myResourceHandlerReceived = false;
  CefRefPtr<CefResourceHandler>  myResourceHandler;
  CefRefPtr<CefCookieAccessFilter> myCookieAccessFilter;

  explicit RemoteResourceRequestHandler(RemoteClientHandler& owner, int id, int peerId);
  IMPLEMENT_REFCOUNTING(RemoteResourceRequestHandler);
};

#endif  // JCEF_REMOTERESOURCEREQUESTHANDLER_H
