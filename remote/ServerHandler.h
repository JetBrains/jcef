#ifndef JCEF_SERVERHANDLER_H
#define JCEF_SERVERHANDLER_H

#include "./gen-cpp/Server.h"

#include "Utils.h"
#include "log/Log.h"

class ServerHandlerContext;

// Used per connection (destroyed when connection closed)
class ServerHandler : public thrift_codegen::ServerIf {
 public:
  ServerHandler();
  ~ServerHandler();

  bool isClosed() const { return myIsClosed; }
  bool isMaster() const { return myIsMaster; }

  //
  // ServerIf
  //
  int32_t connect(const std::string& backwardConnectionPipe, bool isMaster) override;
  int32_t connectTcp(int backwardConnectionPort, bool isMaster) override;
  void log(const std::string& msg) override { Log::info("received message from client: %s", msg.c_str()); }
  void echo(std::string& _return, const std::string& msg) override { _return.assign(msg); }
  void stop() override;
  void state(std::string& _return) override;
  void version(std::string& _return) override;

  //
  // CefBrowser
  //
  int32_t Browser_Create(int cid, int handlersMask, const thrift_codegen::RObject& requestContextHandler) override;
  void Browser_StartNativeCreation(int bid, const std::string& url) override;
  void Browser_Close(const int32_t bid) override;

  void Browser_Reload(const int32_t bid) override;
  void Browser_ReloadIgnoreCache(const int32_t bid) override;
  void Browser_LoadURL(const int32_t bid, const std::string& url) override;
  void Browser_GetURL(std::string& _return, const int32_t bid) override;
  void Browser_ExecuteJavaScript(const int32_t bid,const std::string& code,const std::string& url,const int32_t line) override;
  void Browser_WasResized(const int32_t bid) override;
  void Browser_NotifyScreenInfoChanged(const int32_t bid) override;
  void Browser_SendKeyEvent(const int32_t bid,const int32_t event_type,const int32_t modifiers,const int16_t key_char,const int64_t scanCode,const int32_t key_code) override;
  void Browser_SendMouseEvent(const int32_t bid,const int32_t event_type,const int32_t x,const int32_t y,const int32_t modifiers,const int32_t click_count,const int32_t button) override;
  void Browser_SendMouseWheelEvent(const int32_t bid,const int32_t scroll_type,const int32_t x,const int32_t y,const int32_t modifiers,const int32_t delta,const int32_t units_to_scroll) override;

  void Browser_GoBack(const int32_t bid) override;
  bool Browser_CanGoForward(const int32_t bid) override;
  bool Browser_CanGoBack(const int32_t bid) override;
  void Browser_GoForward(const int32_t bid) override;
  bool Browser_IsLoading(const int32_t bid) override;
  void Browser_StopLoad(const int32_t bid) override;
  int32_t Browser_GetFrameCount(const int32_t bid) override;
  bool Browser_IsPopup(const int32_t bid) override;
  bool Browser_HasDocument(const int32_t bid) override;
  void Browser_ViewSource(const int32_t bid) override;
  void Browser_GetSource(const int32_t bid, const thrift_codegen::RObject& stringVisitor) override;
  void Browser_GetText(const int32_t bid, const thrift_codegen::RObject& stringVisitor) override;
  void Browser_SetFocus(const int32_t bid, bool enable) override;
  double Browser_GetZoomLevel(const int32_t bid) override;
  void Browser_SetZoomLevel(const int32_t bid, const double val) override;
  void Browser_StartDownload(const int32_t bid, const std::string& url) override;
  void Browser_Find(const int32_t bid, const std::string& searchText, const bool forward, const bool matchCase, const bool findNext) override;
  void Browser_StopFinding(const int32_t bid, const bool clearSelection) override;
  void Browser_ReplaceMisspelling(const int32_t bid, const std::string& word) override;
  void Browser_SetFrameRate(const int32_t bid, int32_t val) override;

  //
  // CefFrame
  //
  void Frame_ExecuteJavaScript(const int32_t frameId, const std::string& code, const std::string& url, const int32_t line) override;

  //
  // CefRequest
  //
  void Request_Update(const thrift_codegen::RObject & request) override;
  void Request_GetHeaderByName(std::string& _return,const thrift_codegen::RObject& request,const std::string& name) override;
  void Request_SetHeaderByName(const thrift_codegen::RObject& request,const std::string& name,const std::string& value,const bool overwrite) override;
  void Request_GetHeaderMap(std::map<std::string, std::string>& _return,const thrift_codegen::RObject& request) override;
  void Request_SetHeaderMap(const thrift_codegen::RObject& request,const std::map<std::string, std::string>& headerMap) override;
  void Request_GetPostData(thrift_codegen::PostData& _return,const thrift_codegen::RObject& request) override;
  void Request_SetPostData(const thrift_codegen::RObject& request,const thrift_codegen::PostData& postData) override;

  //
  // CefResponse
  //
  void Response_Update(const thrift_codegen::RObject & response) override;
  void Response_GetHeaderByName(std::string& _return,const thrift_codegen::RObject& response,const std::string& name) override;
  void Response_SetHeaderByName(const thrift_codegen::RObject& response,const std::string& name,const std::string& value,const bool overwrite) override;
  void Response_GetHeaderMap(std::map<std::string, std::string>& _return,const thrift_codegen::RObject& response) override;
  void Response_SetHeaderMap(const thrift_codegen::RObject& response,const std::map<std::string, std::string>& headerMap) override;
  void Request_Set(const thrift_codegen::RObject& request,const std::string& url,const std::string& method,const thrift_codegen::PostData& postData,const std::map<std::string, std::string>& headerMap) override;

  void AuthCallback_Dispose(const thrift_codegen::RObject& authCallback) override;
  void AuthCallback_Continue(const thrift_codegen::RObject& authCallback,const std::string& username,const std::string& password) override;
  void AuthCallback_Cancel(const thrift_codegen::RObject& authCallback) override;
  
  void Callback_Dispose(const thrift_codegen::RObject& callback) override;
  void Callback_Continue(const thrift_codegen::RObject& callback) override;
  void Callback_Cancel(const thrift_codegen::RObject& callback) override;

  //
  // CefMessageRouter
  //
  void MessageRouter_Create(thrift_codegen::RObject& _return,const std::string& query,const std::string& cancel) override;
  void MessageRouter_Dispose(const thrift_codegen::RObject& msgRouter) override;
  void MessageRouter_AddMessageRouterToBrowser(const thrift_codegen::RObject& msgRouter,const int32_t bid) override;
  void MessageRouter_RemoveMessageRouterFromBrowser(const thrift_codegen::RObject& msgRouter,const int32_t bid) override;
  void MessageRouter_AddHandler(const thrift_codegen::RObject& msgRouter,const thrift_codegen::RObject& handler, bool first) override;
  void MessageRouter_RemoveHandler(const thrift_codegen::RObject& msgRouter,const thrift_codegen::RObject& handler) override;
  void MessageRouter_CancelPending(const thrift_codegen::RObject& msgRouter,const int32_t bid,const thrift_codegen::RObject& handler) override;
  
  void QueryCallback_Dispose(const thrift_codegen::RObject& qcallback) override;
  void QueryCallback_Success(const thrift_codegen::RObject& qcallback,const std::string& response) override;
  void QueryCallback_Failure(const thrift_codegen::RObject& qcallback,const int32_t error_code,const std::string& error_message) override;

  //
  // Custom schemes
  //
  void SchemeHandlerFactory_Register(const std::string & schemeName, const std::string & domainName, const thrift_codegen::RObject& schemeHandlerFactory) override;
  void ClearAllSchemeHandlerFactories() override;

  void RequestContext_ClearCertificateExceptions(const int32_t bid, const thrift_codegen::RObject& completionCallback) override;
  void RequestContext_CloseAllConnections(const int32_t bid, const thrift_codegen::RObject& completionCallback) override;

  void CookieManager_Create(thrift_codegen::RObject& _return) override;
  void CookieManager_Dispose(const thrift_codegen::RObject& cookieManager) override;
  bool CookieManager_VisitAllCookies(
      const thrift_codegen::RObject& cookieManager,
      const thrift_codegen::RObject& visitor) override;
  bool CookieManager_VisitUrlCookies(
      const thrift_codegen::RObject& cookieManager,
      const thrift_codegen::RObject& visitor,
      const std::string& url,
      const bool includeHttpOnly) override;
  bool CookieManager_SetCookie(const thrift_codegen::RObject& cookieManager,
                               const std::string& url,
                               const thrift_codegen::Cookie& c) override;
  bool CookieManager_DeleteCookies(const thrift_codegen::RObject& cookieManager,
                                   const std::string& url,
                                   const std::string& cookieName) override;
  bool CookieManager_FlushStore(
      const thrift_codegen::RObject& cookieManager,
      const thrift_codegen::RObject& completionCallback) override;

 private:
  bool myIsMaster = false;
  bool myIsClosed = false;
  std::shared_ptr<ServerHandlerContext> myCtx;

  int connectImpl(std::function<void()> openBackwardTransport);
  void close();
};

#endif  // JCEF_SERVERHANDLER_H
