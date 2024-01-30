#ifndef JCEF_SERVERHANDLER_H
#define JCEF_SERVERHANDLER_H

#include "./gen-cpp/Server.h"

#include "Utils.h"
#include "handlers/RemoteClientHandler.h"
#include "log/Log.h"
#include "router/MessageRoutersManager.h"
#include "browser/ClientsManager.h"

// Used per connection (destroyed when connection closed)
// All methods are invoked in socket-listening thread ("Client_N")
class ServerHandler : public thrift_codegen::ServerIf {
 public:
  ServerHandler();
  ~ServerHandler();

  //
  // ServerIf
  //
  int32_t connect(const std::string& backwardConnectionPipe) override;
  void log(const std::string& msg) override { Log::info("received message from client: %s", msg.c_str()); }
  void echo(std::string& _return, const std::string& msg) override { _return.assign(msg); }

  //
  // CefBrowser
  //
  int32_t createBrowser(int cid, const std::string& url) override;
  void closeBrowser(const int32_t bid) override;

  void Browser_Reload(const int32_t bid) override;
  void Browser_ReloadIgnoreCache(const int32_t bid) override;
  void Browser_LoadURL(const int32_t bid, const std::string& url) override;
  void Browser_GetURL(std::string& _return, const int32_t bid) override;
  void Browser_ExecuteJavaScript(const int32_t bid,const std::string& code,const std::string& url,const int32_t line) override;
  void Browser_WasResized(const int32_t bid,const int32_t width,const int32_t height) override;
  void Browser_SendKeyEvent(const int32_t bid,const int32_t event_type,const int32_t modifiers,const int16_t key_char,const int64_t scanCode,const int32_t key_code) override;
  void Browser_SendMouseEvent(const int32_t bid,const int32_t event_type,const int32_t x,const int32_t y,const int32_t modifiers,const int32_t click_count,const int32_t button) override;
  void Browser_SendMouseWheelEvent(const int32_t bid,const int32_t scroll_type,const int32_t x,const int32_t y,const int32_t modifiers,const int32_t delta,const int32_t units_to_scroll) override;

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

 private:
  std::shared_ptr<RpcExecutor> myJavaService;
  std::shared_ptr<RpcExecutor> myJavaServiceIO;
  std::shared_ptr<ClientsManager> myClientsManager;
  std::shared_ptr<MessageRoutersManager> myRoutersManager;
};

#endif  // JCEF_SERVERHANDLER_H
