#include "ServerHandler.h"

#include <log4cxx/mdc.h>
#include <thread>

#include "browser/CefBrowserAdapter.h"
#include "handlers/RemoteAppHandler.h"
#include "handlers/RemoteLifespanHandler.h"
#include "network/RemotePostData.h"
#include "network/RemoteRequest.h"
#include "network/RemoteResponse.h"

#include "CefUtils.h"
#include "RemoteObjectFactory.h"
#include "callback/RemoteAuthCallback.h"
#include "callback/RemoteCallback.h"

#include "include/base/cef_callback.h"
#include "include/wrapper/cef_closure_task.h"
#include "router/RemoteMessageRouter.h"
#include "router/RemoteMessageRouterHandler.h"
#include "router/RemoteQueryCallback.h"

using namespace apache::thrift;

namespace {
  RemoteAppHandler * g_remoteAppHandler = nullptr;
  std::thread * g_mainCefThread = nullptr;
  bool g_isInitialized = false;
}

bool isCefInitialized() { return g_isInitialized; }

ServerHandler::ServerHandler() : myRoutersManager(std::make_shared<MessageRoutersManager>()) {}

ServerHandler::~ServerHandler() {
  try {
    if (myClientsManager)
      myClientsManager->closeAllBrowsers();
    if (myService && !myService->isClosed())
      myService->close();
    // TODO: probably we should shutdown cef (so AppHandler will update on next intialization)
  } catch (TException e) {
    Log::error("Thrift exception in ~ServerHandler: %s", e.what());
  }
}

int32_t ServerHandler::connect(
    const int32_t backwardConnectionPort,
    const std::vector<std::string>& cmdLineArgs,
    const std::map<std::string, std::string>& settings
) {
  static int s_counter = 0;
  const int cid = s_counter++;
  char buf[64];
  std::sprintf(buf, "Client_%d", cid);
  MDC::put("thread.name", buf);
  Log::debug("Connected new client with cid=%d", cid);

  // Connect to client's side (for cef-callbacks execution on java side)
  if (myService == nullptr) {
    try {
      myService = std::make_shared<RpcExecutor>();
      myClientsManager = std::make_shared<ClientsManager>();
      if (g_remoteAppHandler == nullptr) {
        g_remoteAppHandler = new RemoteAppHandler(myService, cmdLineArgs, settings);
        g_mainCefThread = new std::thread([=]() {
          MDC::put("thread.name", "CefMain");
          CefMainArgs main_args;
          CefSettings cefSettings;
          fillSettings(cefSettings, settings);

          Log::debug("Start CefInitialize");
          const bool success = CefInitialize(main_args, cefSettings, g_remoteAppHandler, nullptr);
          if (!success) {
            Log::error("Cef initialization failed");
            return;
          }
          g_isInitialized = true;
          CefRunMessageLoop();
          Log::debug("Cef shutdowns");
          CefShutdown();
          Log::debug("Shutdown finished");
        });
      } else {
        Log::error("Cef has been initialized and CefApp handler from new client connection will be ignored");
      }
    } catch (TException& tx) {
      Log::error(tx.what());
      return -1;
    }
  }

  return cid;
}

int32_t ServerHandler::createBrowser(int cid) {
  return myClientsManager->createBrowser(cid, myService, myRoutersManager);
}

void ServerHandler::closeBrowser(const int32_t bid) {
  myClientsManager->closeBrowser(bid);
}

void ServerHandler::invoke(const int32_t bid, const std::string& method, const std::string& buffer) {
  auto browser = myClientsManager->getCefBrowser(bid);
  if (browser == nullptr) {
    Log::error("invoke: null browser, bid=%d", bid);
    return;
  }

  CefBrowserAdapter adapter(browser);
  adapter.setBid(bid); // for logging only
  adapter.invoke(method, buffer);
}

void ServerHandler::Request_Update(const thrift_codegen::RObject & request) {
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr)
    return;

  rr->update(request.objInfo);
}

void ServerHandler::Response_Update(const thrift_codegen::RObject& response) {
  RemoteResponse * rr = RemoteResponse::get(response.objId);
  if (rr == nullptr)
    return;

  rr->update(response.objInfo);
}

void ServerHandler::Request_GetHeaderByName(
    std::string& _return,
    const thrift_codegen::RObject& request,
    const std::string& name) {
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr)
    return;

  std::string result = rr->getDelegate()->GetHeaderByName(name).ToString();
  _return.assign(result);
}

void ServerHandler::Request_SetHeaderByName(
    const thrift_codegen::RObject& request,
    const std::string& name,
    const std::string& value,
    const bool overwrite) {
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr)
    return;

  rr->getDelegate()->SetHeaderByName(name, value, overwrite);
}

void ServerHandler::Request_GetHeaderMap(
    std::map<std::string, std::string>& _return,
    const thrift_codegen::RObject& request) {
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr)
    return;

  CefRequest::HeaderMap hmap;
  rr->getDelegate()->GetHeaderMap(hmap);
  fillMap(_return, hmap);
}

void ServerHandler::Request_SetHeaderMap(
    const thrift_codegen::RObject& request,
    const std::map<std::string, std::string>& headerMap) {
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr)
    return;

  CefRequest::HeaderMap hmap;
  fillMap(hmap, headerMap);
  rr->getDelegate()->SetHeaderMap(hmap);
}

void ServerHandler::Response_GetHeaderByName(
    std::string& _return,
    const thrift_codegen::RObject& response,
    const std::string& name) {
  RemoteResponse * rr = RemoteResponse::get(response.objId);
  if (rr == nullptr)
    return;

  std::string result = rr->getDelegate()->GetHeaderByName(name).ToString();
  _return.assign(result);
}

void ServerHandler::Response_SetHeaderByName(
    const thrift_codegen::RObject& response,
    const std::string& name,
    const std::string& value,
    const bool overwrite) {
  RemoteResponse * rr = RemoteResponse::get(response.objId);
  if (rr == nullptr)
    return;

  rr->getDelegate()->SetHeaderByName(name, value, overwrite);
}

void ServerHandler::Response_GetHeaderMap(
    std::map<std::string, std::string>& _return,
    const thrift_codegen::RObject& response) {
  RemoteResponse * rr = RemoteResponse::get(response.objId);
  if (rr == nullptr)
    return;

  CefRequest::HeaderMap hmap;
  rr->getDelegate()->GetHeaderMap(hmap);
  fillMap(_return, hmap);
}

void ServerHandler::Response_SetHeaderMap(
    const thrift_codegen::RObject& response,
    const std::map<std::string, std::string>& headerMap) {
  RemoteResponse * rr = RemoteResponse::get(response.objId);
  if (rr == nullptr)
    return;

  CefRequest::HeaderMap hmap;
  fillMap(hmap, headerMap);
  rr->getDelegate()->SetHeaderMap(hmap);
}

void ServerHandler::Request_GetPostData(
    thrift_codegen::PostData& _return,
    const thrift_codegen::RObject& request
) {
  _return.isReadOnly = true;
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr) return;

  CefRefPtr<CefPostData> pd = rr->getDelegate()->GetPostData();
  if (!pd) return;

  _return.isReadOnly = pd->IsReadOnly();
  _return.hasExcludedElements = pd->HasExcludedElements();
  if (pd->GetElementCount() > 0) {
    CefPostData::ElementVector elements;
    pd->GetElements(elements);
    for (auto e : elements) {
      thrift_codegen::PostDataElement ee;
      ee.isReadOnly = e->IsReadOnly();
      if (e->GetType() == PDE_TYPE_FILE)
        ee.file = e->GetFile();
      else if (e->GetType() == PDE_TYPE_BYTES) {
        if (e->GetBytesCount() > 0) {
          char* buf = new char[e->GetBytesCount()];
          e->GetBytes(e->GetBytesCount(), buf);
          ee.bytes.assign((const char*)buf, e->GetBytesCount());
          delete[] buf;
        }
      }
      _return.elements.push_back(ee);
    }
  }
}

void ServerHandler::Request_SetPostData(
    const thrift_codegen::RObject& request,
    const thrift_codegen::PostData& postData) {
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr)
    return;

  CefRefPtr<CefPostData> pd = new RemotePostData(postData);
  rr->getDelegate()->SetPostData(pd);
}

void ServerHandler::Request_Set(
    const thrift_codegen::RObject& request,
    const std::string& url,
    const std::string& method,
    const thrift_codegen::PostData& postData,
    const std::map<std::string, std::string>& headerMap) {
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr)
    return;

  CefRefPtr<CefPostData> pd = new RemotePostData(postData);
  CefRequest::HeaderMap hmap;
  fillMap(hmap, headerMap);
  rr->getDelegate()->Set(url, method, pd, hmap);
}

void ServerHandler::AuthCallback_Dispose(const thrift_codegen::RObject& authCallback) {
  RemoteAuthCallback::dispose(authCallback.objId);
}

void ServerHandler::AuthCallback_Continue(
    const thrift_codegen::RObject& authCallback,
    const std::string& username,
    const std::string& password
) {
  RemoteAuthCallback * rc = RemoteAuthCallback::get(authCallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate()->Continue(username, password);
  RemoteAuthCallback::dispose(authCallback.objId);
}

void ServerHandler::AuthCallback_Cancel(const thrift_codegen::RObject& authCallback) {
  RemoteAuthCallback * rc = RemoteAuthCallback::get(authCallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate()->Cancel();
  RemoteAuthCallback::dispose(authCallback.objId);
}

void ServerHandler::Callback_Dispose(const thrift_codegen::RObject& callback) {
  RemoteCallback::dispose(callback.objId);
}

void ServerHandler::Callback_Continue(const thrift_codegen::RObject& callback) {
  RemoteCallback * rc = RemoteCallback::get(callback.objId);
  if (rc == nullptr) return;
  rc->getDelegate()->Continue();
  RemoteCallback::dispose(callback.objId);
}

void ServerHandler::Callback_Cancel(const thrift_codegen::RObject& callback) {
  RemoteCallback * rc = RemoteCallback::get(callback.objId);
  if (rc == nullptr) return;
  rc->getDelegate()->Cancel();
  RemoteCallback::dispose(callback.objId);
}

// TODO: add leak protection: dispose all created routers is ~ServerHandler
void ServerHandler::MessageRouter_Create(thrift_codegen::RObject& _return,
                                        const std::string& query,
                                        const std::string& cancel) {
  _return = myRoutersManager->CreateRemoteMessageRouter(myService, query, cancel)->toThrift();
}

void ServerHandler::MessageRouter_Dispose(const thrift_codegen::RObject& msgRouter) {
  myRoutersManager->DisposeRemoteMessageRouter(msgRouter.objId);
}

void ServerHandler::MessageRouter_AddMessageRouterToBrowser(
    const thrift_codegen::RObject& msgRouter,
    const int32_t bid) {
  LNDCT();
  RemoteMessageRouter * rmr = RemoteMessageRouter::get(msgRouter.objId);
  if (rmr == nullptr) return;

  // Update running render-processes.
  CefRefPtr<CefBrowser> browser = myClientsManager->getCefBrowser(bid);
  if (!browser) {
    Log::debug("CefBrowser instance wasn't created, bid %d", bid);
    return;
  }

  CefRefPtr<CefProcessMessage> message = CefProcessMessage::Create("AddMessageRouter");
  CefRefPtr<CefListValue> args = message->GetArgumentList();
  const CefMessageRouterConfig& config = rmr->getConfig();
  args->SetString(0, config.js_query_function);
  args->SetString(1, config.js_cancel_function);

  browser->GetMainFrame()->SendProcessMessage(PID_RENDERER, message);
}

void ServerHandler::MessageRouter_RemoveMessageRouterFromBrowser(
    const thrift_codegen::RObject& msgRouter,
    const int32_t bid) {
  LNDCT();
  RemoteMessageRouter * rmr = RemoteMessageRouter::get(msgRouter.objId);
  if (rmr == nullptr) return;

  // Update running render-processes.
  CefRefPtr<CefBrowser> browser = myClientsManager->getCefBrowser(bid);
  if (!browser) {
    Log::debug("CefBrowser instance wasn't created, bid %d", bid);
    return;
  }

  CefRefPtr<CefProcessMessage> message = CefProcessMessage::Create("RemoveMessageRouter");
  CefRefPtr<CefListValue> args = message->GetArgumentList();
  const CefMessageRouterConfig& config = rmr->getConfig();
  args->SetString(0, config.js_query_function);
  args->SetString(1, config.js_cancel_function);

  browser->GetMainFrame()->SendProcessMessage(PID_RENDERER, message);
}

namespace {
  void ServerHandler_MessageRouter_AddHandler_Impl(
      std::shared_ptr<RpcExecutor> service,
      std::shared_ptr<ClientsManager> manager,
      const thrift_codegen::RObject& msgRouter,
      const thrift_codegen::RObject& handler, bool first) {
    LNDCT();
    RemoteMessageRouter * rmr = RemoteMessageRouter::get(msgRouter.objId);
    if (rmr == nullptr) {
      Log::error("Can't find router %d", msgRouter.objId);
      return;
    }
    rmr->AddRemoteHandler(manager, handler, first);
  }
  void ServerHandler_MessageRouter_RemoveHandler_Impl(
      const thrift_codegen::RObject& msgRouter,
      const thrift_codegen::RObject& handler) {
    LNDCT();
    RemoteMessageRouter * rmr = RemoteMessageRouter::get(msgRouter.objId);
    if (rmr != nullptr) {
      rmr->RemoveRemoteHandler(handler);
    } else
      Log::error("Can't find router %d", msgRouter.objId);

    RemoteMessageRouterHandler::dispose(handler.objId); // should be called in RemoveRemoteHandler, just for insurance
  }
}

void ServerHandler::MessageRouter_AddHandler(
    const thrift_codegen::RObject& msgRouter,
    const thrift_codegen::RObject& handler, bool first) {
  if (CefCurrentlyOn(TID_UI)) {
    ServerHandler_MessageRouter_AddHandler_Impl(myService, myClientsManager, msgRouter, handler, first);
  } else {
    CefPostTask(TID_UI, base::BindOnce(
        [](std::shared_ptr<RpcExecutor> service,
           std::shared_ptr<ClientsManager> manager,
           const thrift_codegen::RObject& msgRouter,
           const thrift_codegen::RObject& handler,
           bool first) {
          ServerHandler_MessageRouter_AddHandler_Impl(service, manager, msgRouter, handler, first);
        },
        myService, myClientsManager, msgRouter, handler, first));
  }
}

void ServerHandler::MessageRouter_RemoveHandler(
    const thrift_codegen::RObject& msgRouter,
    const thrift_codegen::RObject& handler) {
  if (CefCurrentlyOn(TID_UI)) {
    ServerHandler_MessageRouter_RemoveHandler_Impl(msgRouter, handler);
  } else {
    CefPostTask(TID_UI, base::BindOnce(
        [](
           const thrift_codegen::RObject& msgRouter,
           const thrift_codegen::RObject& handler) {
          ServerHandler_MessageRouter_RemoveHandler_Impl(msgRouter, handler);
        },
        msgRouter, handler));
  }
}

void ServerHandler::MessageRouter_CancelPending(
    const thrift_codegen::RObject& msgRouter,
    const int32_t bid,
    const thrift_codegen::RObject& handler) {
  LNDCT();
  RemoteMessageRouter * rmr = RemoteMessageRouter::get(msgRouter.objId);
  if (rmr == nullptr) return;
  RemoteMessageRouterHandler * rmrh = RemoteMessageRouterHandler::find(handler.objId);
  if (rmrh != nullptr) {
    rmr->getDelegate()->CancelPending(myClientsManager->getCefBrowser(bid), rmrh);
  } else
    Log::error("Can't find RemoteMessageRouterHandler %d", handler.objId);
}

void ServerHandler::QueryCallback_Dispose(const thrift_codegen::RObject& qcallback) {
  RemoteQueryCallback::dispose(qcallback.objId);
}

void ServerHandler::QueryCallback_Success(
    const thrift_codegen::RObject& qcallback,
    const std::string& response) {
  RemoteQueryCallback * rc = RemoteQueryCallback::get(qcallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate()->Success(response);
  RemoteQueryCallback::dispose(qcallback.objId);
}

void ServerHandler::QueryCallback_Failure(
    const thrift_codegen::RObject& qcallback,
    const int32_t error_code,
    const std::string& error_message) {
  RemoteQueryCallback * rc = RemoteQueryCallback::get(qcallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate()->Failure(error_code, error_message);
  RemoteQueryCallback::dispose(qcallback.objId);
}
