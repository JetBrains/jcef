// This autogenerated skeleton file illustrates how to build a server.
// You should copy it to another filename to avoid overwriting it.

#include "ClientHandlers.h"
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TSimpleServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TBufferTransports.h>

using namespace ::apache::thrift;
using namespace ::apache::thrift::protocol;
using namespace ::apache::thrift::transport;
using namespace ::apache::thrift::server;

using namespace  ::thrift_codegen;

class ClientHandlersHandler : virtual public ClientHandlersIf {
 public:
  ClientHandlersHandler() {
    // Your initialization goes here
  }

  int32_t connect() {
    // Your implementation goes here
    printf("connect\n");
  }

  void log(const std::string& msg) {
    // Your implementation goes here
    printf("log\n");
  }

  void AppHandler_GetRegisteredCustomSchemes(std::vector<CustomScheme> & _return) {
    // Your implementation goes here
    printf("AppHandler_GetRegisteredCustomSchemes\n");
  }

  void AppHandler_OnContextInitialized() {
    // Your implementation goes here
    printf("AppHandler_OnContextInitialized\n");
  }

  void RenderHandler_GetViewRect(Rect& _return, const int32_t bid) {
    // Your implementation goes here
    printf("RenderHandler_GetViewRect\n");
  }

  void RenderHandler_GetScreenInfo(ScreenInfo& _return, const int32_t bid) {
    // Your implementation goes here
    printf("RenderHandler_GetScreenInfo\n");
  }

  void RenderHandler_GetScreenPoint(Point& _return, const int32_t bid, const int32_t viewX, const int32_t viewY) {
    // Your implementation goes here
    printf("RenderHandler_GetScreenPoint\n");
  }

  void RenderHandler_OnPaint(const int32_t bid, const bool popup, const int32_t dirtyRectsCount, const std::string& sharedMemName, const int64_t sharedMemHandle, const bool recreateHandle, const int32_t width, const int32_t height) {
    // Your implementation goes here
    printf("RenderHandler_OnPaint\n");
  }

  bool LifeSpanHandler_OnBeforePopup(const int32_t bid, const std::string& url, const std::string& frameName, const bool gesture) {
    // Your implementation goes here
    printf("LifeSpanHandler_OnBeforePopup\n");
  }

  void LifeSpanHandler_OnAfterCreated(const int32_t bid) {
    // Your implementation goes here
    printf("LifeSpanHandler_OnAfterCreated\n");
  }

  bool LifeSpanHandler_DoClose(const int32_t bid) {
    // Your implementation goes here
    printf("LifeSpanHandler_DoClose\n");
  }

  void LifeSpanHandler_OnBeforeClose(const int32_t bid) {
    // Your implementation goes here
    printf("LifeSpanHandler_OnBeforeClose\n");
  }

  void LoadHandler_OnLoadingStateChange(const int32_t bid, const bool isLoading, const bool canGoBack, const bool canGoForward) {
    // Your implementation goes here
    printf("LoadHandler_OnLoadingStateChange\n");
  }

  void LoadHandler_OnLoadStart(const int32_t bid, const int32_t transition_type) {
    // Your implementation goes here
    printf("LoadHandler_OnLoadStart\n");
  }

  void LoadHandler_OnLoadEnd(const int32_t bid, const int32_t httpStatusCode) {
    // Your implementation goes here
    printf("LoadHandler_OnLoadEnd\n");
  }

  void LoadHandler_OnLoadError(const int32_t bid, const int32_t errorCode, const std::string& errorText, const std::string& failedUrl) {
    // Your implementation goes here
    printf("LoadHandler_OnLoadError\n");
  }

  void DisplayHandler_OnAddressChange(const int32_t bid, const std::string& url) {
    // Your implementation goes here
    printf("DisplayHandler_OnAddressChange\n");
  }

  void DisplayHandler_OnTitleChange(const int32_t bid, const std::string& title) {
    // Your implementation goes here
    printf("DisplayHandler_OnTitleChange\n");
  }

  bool DisplayHandler_OnTooltip(const int32_t bid, const std::string& text) {
    // Your implementation goes here
    printf("DisplayHandler_OnTooltip\n");
  }

  void DisplayHandler_OnStatusMessage(const int32_t bid, const std::string& value) {
    // Your implementation goes here
    printf("DisplayHandler_OnStatusMessage\n");
  }

  bool DisplayHandler_OnConsoleMessage(const int32_t bid, const int32_t level, const std::string& message, const std::string& source, const int32_t line) {
    // Your implementation goes here
    printf("DisplayHandler_OnConsoleMessage\n");
  }

  bool RequestHandler_OnBeforeBrowse(const int32_t bid, const  ::thrift_codegen::RObject& request, const bool user_gesture, const bool is_redirect) {
    // Your implementation goes here
    printf("RequestHandler_OnBeforeBrowse\n");
  }

  bool RequestHandler_OnOpenURLFromTab(const int32_t bid, const std::string& target_url, const bool user_gesture) {
    // Your implementation goes here
    printf("RequestHandler_OnOpenURLFromTab\n");
  }

  bool RequestHandler_GetAuthCredentials(const int32_t bid, const std::string& origin_url, const bool isProxy, const std::string& host, const int32_t port, const std::string& realm, const std::string& scheme, const  ::thrift_codegen::RObject& authCallback) {
    // Your implementation goes here
    printf("RequestHandler_GetAuthCredentials\n");
  }

  bool RequestHandler_OnCertificateError(const int32_t bid, const std::string& cert_error, const std::string& request_url, const  ::thrift_codegen::RObject& sslInfo, const  ::thrift_codegen::RObject& callback) {
    // Your implementation goes here
    printf("RequestHandler_OnCertificateError\n");
  }

  void RequestHandler_OnRenderProcessTerminated(const int32_t bid, const std::string& status) {
    // Your implementation goes here
    printf("RequestHandler_OnRenderProcessTerminated\n");
  }

  void RequestHandler_GetResourceRequestHandler( ::thrift_codegen::RObject& _return, const int32_t bid, const  ::thrift_codegen::RObject& request, const bool isNavigation, const bool isDownload, const std::string& requestInitiator) {
    // Your implementation goes here
    printf("RequestHandler_GetResourceRequestHandler\n");
  }

  void ResourceRequestHandler_Dispose(const int32_t rrHandler) {
    // Your implementation goes here
    printf("ResourceRequestHandler_Dispose\n");
  }

  void ResourceRequestHandler_GetCookieAccessFilter( ::thrift_codegen::RObject& _return, const int32_t rrHandler, const int32_t bid, const  ::thrift_codegen::RObject& request) {
    // Your implementation goes here
    printf("ResourceRequestHandler_GetCookieAccessFilter\n");
  }

  void CookieAccessFilter_Dispose(const int32_t filter) {
    // Your implementation goes here
    printf("CookieAccessFilter_Dispose\n");
  }

  bool CookieAccessFilter_CanSendCookie(const int32_t filter, const int32_t bid, const  ::thrift_codegen::RObject& request, const std::vector<std::string> & cookie) {
    // Your implementation goes here
    printf("CookieAccessFilter_CanSendCookie\n");
  }

  bool CookieAccessFilter_CanSaveCookie(const int32_t filter, const int32_t bid, const  ::thrift_codegen::RObject& request, const  ::thrift_codegen::RObject& response, const std::vector<std::string> & cookie) {
    // Your implementation goes here
    printf("CookieAccessFilter_CanSaveCookie\n");
  }

  bool ResourceRequestHandler_OnBeforeResourceLoad(const int32_t rrHandler, const int32_t bid, const  ::thrift_codegen::RObject& request) {
    // Your implementation goes here
    printf("ResourceRequestHandler_OnBeforeResourceLoad\n");
  }

  void ResourceRequestHandler_GetResourceHandler( ::thrift_codegen::RObject& _return, const int32_t rrHandler, const int32_t bid, const  ::thrift_codegen::RObject& request) {
    // Your implementation goes here
    printf("ResourceRequestHandler_GetResourceHandler\n");
  }

  void ResourceHandler_Dispose(const int32_t resourceHandler) {
    // Your implementation goes here
    printf("ResourceHandler_Dispose\n");
  }

  void ResourceRequestHandler_OnResourceRedirect(std::string& _return, const int32_t rrHandler, const int32_t bid, const  ::thrift_codegen::RObject& request, const  ::thrift_codegen::RObject& response, const std::string& new_url) {
    // Your implementation goes here
    printf("ResourceRequestHandler_OnResourceRedirect\n");
  }

  bool ResourceRequestHandler_OnResourceResponse(const int32_t rrHandler, const int32_t bid, const  ::thrift_codegen::RObject& request, const  ::thrift_codegen::RObject& response) {
    // Your implementation goes here
    printf("ResourceRequestHandler_OnResourceResponse\n");
  }

  void ResourceRequestHandler_OnResourceLoadComplete(const int32_t rrHandler, const int32_t bid, const  ::thrift_codegen::RObject& request, const  ::thrift_codegen::RObject& response, const std::string& status, const int64_t receivedContentLength) {
    // Your implementation goes here
    printf("ResourceRequestHandler_OnResourceLoadComplete\n");
  }

  bool ResourceRequestHandler_OnProtocolExecution(const int32_t rrHandler, const int32_t bid, const  ::thrift_codegen::RObject& request, const bool allowOsExecution) {
    // Your implementation goes here
    printf("ResourceRequestHandler_OnProtocolExecution\n");
  }

  bool MessageRouterHandler_onQuery(const  ::thrift_codegen::RObject& handler, const int32_t bid, const int64_t queryId, const std::string& request, const bool persistent, const  ::thrift_codegen::RObject& queryCallback) {
    // Your implementation goes here
    printf("MessageRouterHandler_onQuery\n");
  }

  void MessageRouterHandler_onQueryCanceled(const  ::thrift_codegen::RObject& handler, const int32_t bid, const int64_t queryId) {
    // Your implementation goes here
    printf("MessageRouterHandler_onQueryCanceled\n");
  }

};

int main(int argc, char **argv) {
  int port = 9090;
  ::std::shared_ptr<ClientHandlersHandler> handler(new ClientHandlersHandler());
  ::std::shared_ptr<TProcessor> processor(new ClientHandlersProcessor(handler));
  ::std::shared_ptr<TServerTransport> serverTransport(new TServerSocket(port));
  ::std::shared_ptr<TTransportFactory> transportFactory(new TBufferedTransportFactory());
  ::std::shared_ptr<TProtocolFactory> protocolFactory(new TBinaryProtocolFactory());

  TSimpleServer server(processor, serverTransport, transportFactory, protocolFactory);
  server.serve();
  return 0;
}

