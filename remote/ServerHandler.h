#ifndef JCEF_SERVERHANDLER_H
#define JCEF_SERVERHANDLER_H

#include "./gen-cpp/Server.h"

#include "Utils.h"
#include "handlers/RemoteClientHandler.h"
#include "log/Log.h"

// Used per connection (destroyed when connection closed)
// All methods are invoked in socket-listening thread ("Client_N")
class ServerHandler : public thrift_codegen::ServerIf {
 public:
  ~ServerHandler();

  //
  // ServerIf
  //
  int32_t connect(const int32_t backwardConnectionPort,
                  const std::vector<std::string>& cmdLineArgs,
                  const std::map<std::string, std::string>& settings) override;
  void log(const std::string& msg) override {
    Log::info("received message from client: %s", msg.c_str());
  }

  //
  // CefBrowser
  //
  int32_t createBrowser(int cid) override;
  void closeBrowser(std::string& _return, const int32_t bid) override;
  void invoke(const int32_t bid, const std::string& method, const std::string& buffer) override;

  //
  // CefRequest
  //
  void Request_Update(const thrift_codegen::RObject & request) override;


  void Request_GetHeaderByName(std::string& _return,
                                  const thrift_codegen::RObject& request,
                                  const std::string& name) override;
  void Request_SetHeaderByName(const thrift_codegen::RObject& request,
                                  const std::string& name,
                                  const std::string& value,
                                  const bool overwrite) override;
  void Request_GetHeaderMap(std::map<std::string, std::string>& _return,
                               const thrift_codegen::RObject& request) override;
  void Request_SetHeaderMap(
      const thrift_codegen::RObject& request,
      const std::map<std::string, std::string>& headerMap) override;

  void Request_GetPostData(thrift_codegen::PostData& _return,
                              const thrift_codegen::RObject& request) override;
  void Request_SetPostData(
      const thrift_codegen::RObject& request,
      const thrift_codegen::PostData& postData) override;

  //
  // CefResponse
  //
  void Response_Update(const thrift_codegen::RObject & response) override;
  void Response_GetHeaderByName(std::string& _return,
                                   const thrift_codegen::RObject& response,
                                   const std::string& name) override;
  void Response_SetHeaderByName(const thrift_codegen::RObject& response,
                                   const std::string& name,
                                   const std::string& value,
                                   const bool overwrite) override;
  void Response_GetHeaderMap(
      std::map<std::string, std::string>& _return,
      const thrift_codegen::RObject& response) override;
  void Response_SetHeaderMap(
      const thrift_codegen::RObject& response,
      const std::map<std::string, std::string>& headerMap) override;
  void Request_Set(
      const thrift_codegen::RObject& request,
      const std::string& url,
      const std::string& method,
      const thrift_codegen::PostData& postData,
      const std::map<std::string, std::string>& headerMap) override;

  void AuthCallback_Dispose(
      const thrift_codegen::RObject& authCallback) override;
  void AuthCallback_Continue(const thrift_codegen::RObject& authCallback,
                                const std::string& username,
                                const std::string& password) override;
  void AuthCallback_Cancel(
      const thrift_codegen::RObject& authCallback) override;
  void Callback_Dispose(const thrift_codegen::RObject& callback) override;
  void Callback_Continue(const thrift_codegen::RObject& callback) override;
  void Callback_Cancel(const thrift_codegen::RObject& callback) override;

  void CreateMessageRouter(thrift_codegen::RObject& _return,
                           const std::string& query,
                           const std::string& cancel) override;
  void MessageRouter_Dispose(const thrift_codegen::RObject& msgRouter) override;
  void MessageRouter_AddMessageRouterToBrowser(
      const thrift_codegen::RObject& msgRouter,
      const int32_t bid) override;
  void MessageRouter_RemoveMessageRouterFromBrowser(
      const thrift_codegen::RObject& msgRouter,
      const int32_t bid) override;
  void MessageRouter_AddHandler(
      const thrift_codegen::RObject& msgRouter,
      const thrift_codegen::RObject& handler) override;
  void MessageRouter_RemoveHandler(
      const thrift_codegen::RObject& msgRouter,
      const thrift_codegen::RObject& handler) override;
  void MessageRouter_CancelPending(
      const thrift_codegen::RObject& msgRouter,
      const int32_t bid,
      const thrift_codegen::RObject& handler) override;
  void QueryCallback_Dispose(const thrift_codegen::RObject& qcallback) override;
  void QueryCallback_Success(const thrift_codegen::RObject& qcallback,
                             const std::string& response) override;
  void QueryCallback_Failure(const thrift_codegen::RObject& qcallback,
                             const int32_t error_code,
                             const std::string& error_message) override;

 private:
  CefRefPtr<CefBrowser> getBrowser(int bid);
  void closeAllBrowsers();

  std::shared_ptr<std::vector<CefRefPtr<RemoteClientHandler>>> myRemoteBrowsers;
  std::shared_ptr<RpcExecutor> myService;
};

#endif  // JCEF_SERVERHANDLER_H
