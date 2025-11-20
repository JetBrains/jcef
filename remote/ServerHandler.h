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

  bool isMaster() const { return myIsMaster; }

  //
  // ServerIf
  //
  int32_t connect(const std::string& backwardConnectionPipe, bool isMaster) override;
  int32_t connectTcp(int backwardConnectionPort, bool isMaster) override;
  void attach(int connectionId) override;
  void log(const std::string& msg) override { Log::info("received message from client: %s", msg.c_str()); }
  void echo(std::string& _return, const std::string& msg) override { _return.assign(msg); }
  void getServerInfo(std::string& _return, const std::string& request) override;

  //
  // CefClient
  //
  int32_t Client_Create(int handlersMask) override;
  void    Client_Dispose(int cid) override;
  void    Client_AddHandlers(int cid, int handlersMask) override;
  void    Client_RemoveHandlers(int cid, int handlersMask) override;
  void    Client_AddMessageRouter(int cid, const thrift_codegen::RObject& msgRouter) override;
  void    Client_RemoveMessageRouter(int cid, const thrift_codegen::RObject& msgRouter) override;

  //
  // CefBrowser
  //
  int32_t Browser_Create(int cid, const thrift_codegen::RObject& requestContextHandler) override;
  void Browser_StartNativeCreation(int bid, const std::string& url) override;
  void Browser_OpenDevTools(int bid, int x, int y) override;
  void Browser_Close(const int32_t bid) override;
  void Browser_CloseDevTools(const int32_t bid) override;

  void Browser_Reload(const int32_t bid) override;
  void Browser_ReloadIgnoreCache(const int32_t bid) override;
  void Browser_LoadURL(const int32_t bid, const std::string& url) override;
  void Browser_LoadRequest(const int32_t bid, const thrift_codegen::RObject & request) override;
  void Browser_GetURL(std::string& _return, const int32_t bid) override;
  void Browser_ExecuteJavaScript(const int32_t bid,const std::string& code,const std::string& url,const int32_t line) override;
  void Browser_WasResized(const int32_t bid) override;
  void Browser_NotifyScreenInfoChanged(const int32_t bid) override;
  void Browser_Invalidate(const int32_t bid) override;
  void Browser_SendCefKeyEvent(const int32_t bid, const thrift_codegen::CefKeyEventAttributes& event) override;
  void Browser_SendMouseEvent(const int32_t bid,const int32_t event_type,const int32_t x,const int32_t y,const int32_t modifiers,const int32_t click_count,const int32_t button) override;
  void Browser_SendMouseWheelEvent(const int32_t bid,const int32_t scroll_type,const int32_t x,const int32_t y,const int32_t modifiers,const int32_t delta,const int32_t units_to_scroll) override;

  void Browser_GoBack(const int32_t bid) override;
  bool Browser_CanGoForward(const int32_t bid) override;
  bool Browser_CanGoBack(const int32_t bid) override;
  void Browser_GoForward(const int32_t bid) override;
  bool Browser_IsLoading(const int32_t bid) override;
  void Browser_StopLoad(const int32_t bid) override;
  void Browser_GetMainFrame(thrift_codegen::RObject& _return, const int32_t bid) override;
  void Browser_GetFocusedFrame(thrift_codegen::RObject& _return, const int32_t bid) override;
  void Browser_GetFrameByIdentifier(thrift_codegen::RObject& _return, const int32_t bid, const std::string& id) override;
  void Browser_GetFrameByName(thrift_codegen::RObject& _return, const int32_t bid, const std::string& name) override;
  void Browser_GetFrameIdentifiers(std::vector<std::string>& _return, const int32_t bid) override;
  void Browser_GetFrameNames(std::vector<std::string>& _return, const int32_t bid) override;
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
  void Browser_AddDevToolsMessageObserver(
      thrift_codegen::RObject& _return,
      const int32_t bid,
      const thrift_codegen::RObject& observer) override;
  void Browser_ExecuteDevToolsMethod(
      const int32_t bid,
      const std::string& method,
      const std::string& parametersAsJson,
      const thrift_codegen::RObject& intCallback) override;
  void Browser_RunFileDialog(
      const int32_t bid,
      const std::string& mode,
      const std::string& title,
      const std::string& defaultFilePath,
      const std::vector<std::string>& acceptFilters,
      const thrift_codegen::RObject& runFileDialogCallback) override;
  void Browser_PrintToPDF(
      const int32_t bid,
      const std::string& path,
      const std::map<std::string, std::string>& pdfPrintSettings,
      const thrift_codegen::RObject& pdfPrintCallback) override;
  void Browser_Print(const int32_t bid) override;
  void Browser_ImeSetComposition(
      const int32_t bid,
      const std::string& text,
      const std::vector<thrift_codegen::CompositionUnderline>& underlines,
      const thrift_codegen::Range& replacementRange,
      const thrift_codegen::Range& selectionRange) override;
  void Browser_ImeCommitText(const int32_t bid,
                             const std::string& text,
                             const thrift_codegen::Range& replacementRange,
                             const int32_t relativeCursorPos) override;
  void Browser_ImeFinishComposingText(const int32_t bid, const bool keepSelection) override;
  void Browser_ImeCancelComposing(const int32_t bid) override;

  //
  // CefFrame
  //
  void Frame_ExecuteJavaScript(const int32_t frameId, const std::string& code, const std::string& url, const int32_t line) override;
  void Frame_Dispose(const int32_t frameId) override;
  void Frame_GetParent(thrift_codegen::RObject & _return, int frameId) override;
  void Frame_Undo(int frameId) override;
  void Frame_Redo(int frameId) override;
  void Frame_Cut(int frameId) override;
  void Frame_Copy(int frameId) override;
  void Frame_Paste(int frameId) override;
  void Frame_Delete(int frameId) override;
  void Frame_SelectAll(int frameId) override;

  //
  // CefRequest
  //
  void Request_Create(thrift_codegen::RObject& result) override;
  void Request_Dispose(int requestId) override;
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

  void CefRunContextMenuCallback_Dispose(const thrift_codegen::RObject& self) override;
  void CefRunContextMenuCallback_Continue(const thrift_codegen::RObject& self, const int32_t command_id, const int32_t event_flag) override;
  void CefRunContextMenuCallback_Cancel(const thrift_codegen::RObject& self) override;

  //
  // CefMessageRouter
  //
  void MessageRouter_Create(thrift_codegen::RObject& _return,const std::string& query,const std::string& cancel) override;
  void MessageRouter_Dispose(const thrift_codegen::RObject& msgRouter) override;
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

  void Registration_Dispose(
      const thrift_codegen::RObject& registration) override;

  // CefMediaAccessCallback
  void MediaAccessCallback_Dispose(const thrift_codegen::RObject& mediaAccessCallback) override;
  void MediaAccessCallback_Continue(const thrift_codegen::RObject& mediaAccessCallback, const int32_t allowed_permissions) override;
  void MediaAccessCallback_Cancel( const thrift_codegen::RObject& mediaAccessCallback) override;

  std::shared_ptr<ServerHandlerContext> getCtx() const { return myCtx; }
  int getCid() const { return myCid; }

 private:
  bool myIsMaster = false;
  int myCid = -1;
  std::shared_ptr<ServerHandlerContext> myCtx;

  int connectImpl(std::function<void()> openBackwardTransport);
};

#endif  // JCEF_SERVERHANDLER_H
