#ifndef JCEF_REMOTEREQUESTCONTEXTHANDLER_H
#define JCEF_REMOTEREQUESTCONTEXTHANDLER_H

#include "include/cef_request_context_handler.h"
#include "../RemoteObjects.h"

class ServerHandlerContext;

// Created in RemoteClientHandler::ctor (when RemoteBrowser created)
// Owned by native CefRequestContext object (lifetime if managed by CefRefPtr) that is owned by RemoteClientHandler
// Java-peer is owned by RemoteBrowser (disposed with RemoteBrowser via usual gc)
class RemoteRequestContextHandler: public CefRequestContextHandler, public RemoteJavaObject<RemoteRequestContextHandler> {
 public:
  explicit RemoteRequestContextHandler(std::shared_ptr<ServerHandlerContext> ctx, thrift_codegen::RObject peer);

  ///
  /// Called on the browser process IO thread before a resource request is
  /// initiated. The |browser| and |frame| values represent the source of the
  /// request, and may be NULL for requests originating from service workers or
  /// CefURLRequest. |request| represents the request contents and cannot be
  /// modified in this callback. |is_navigation| will be true if the resource
  /// request is a navigation. |is_download| will be true if the resource
  /// request is a download. |request_initiator| is the origin (scheme + domain)
  /// of the page that initiated the request. Set |disable_default_handling| to
  /// true to disable default handling of the request, in which case it will
  /// need to be handled via CefResourceRequestHandler::GetResourceHandler or it
  /// will be canceled. To allow the resource load to proceed with default
  /// handling return NULL. To specify a handler for the resource return a
  /// CefResourceRequestHandler object. This method will not be called if the
  /// client associated with |browser| returns a non-NULL value from
  /// CefRequestHandler::GetResourceRequestHandler for the same request
  /// (identified by CefRequest::GetIdentifier).
  ///
  /*--cef(optional_param=browser,optional_param=frame,
          optional_param=request_initiator)--*/
  virtual CefRefPtr<CefResourceRequestHandler> GetResourceRequestHandler(
      CefRefPtr<CefBrowser> browser,
      CefRefPtr<CefFrame> frame,
      CefRefPtr<CefRequest> request,
      bool is_navigation,
      bool is_download,
      const CefString& request_initiator,
      bool& disable_default_handling) override;

 private:
  std::shared_ptr<ServerHandlerContext> myCtx;

  IMPLEMENT_REFCOUNTING(RemoteRequestContextHandler);
};

#endif  // JCEF_REMOTEREQUESTCONTEXTHANDLER_H
