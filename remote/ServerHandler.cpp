#include "ServerHandler.h"

#include "include/cef_version.h"

#include "handlers/app/RemoteAppHandler.h"
#include "network/RemotePostData.h"
#include "network/RemoteRequest.h"
#include "network/RemoteResponse.h"

#include "RemoteObjects.h"
#include "callback/RemoteAuthCallback.h"
#include "callback/RemoteCallback.h"

#include "include/base/cef_callback.h"
#include "include/wrapper/cef_closure_task.h"
#include "router/RemoteMessageRouter.h"
#include "router/RemoteMessageRouterHandler.h"
#include "router/RemoteQueryCallback.h"

#include "ServerState.h"

using namespace apache::thrift;

ServerHandler::ServerHandler()
    : myRoutersManager(std::make_shared<MessageRoutersManager>()),
      myClientsManager(std::make_shared<ClientsManager>())
{}

ServerHandler::~ServerHandler() {
  close();
  closeBackwardTransport();
}

void ServerHandler::close() {
  if (myIsClosed)
    return;

  myIsClosed = true;
  std::string remaining = myClientsManager->closeAllBrowsers();
  ServerState::instance().onServerHandlerClosed(*this, remaining);
  try {
    // NOTE: if some browser wasn't closed than client won't receive onBeforeClose callback
    // if we close transport here. So do it in destructor.
    if (remaining.empty())
      closeBackwardTransport();
  } catch (TException& e) {
    Log::error("Thrift exception in ServerHandler::close: %s", e.what());
  }
}

void ServerHandler::closeBackwardTransport() {
  if (myJavaService && !myJavaService->isClosed())
    myJavaService->close();
  if (myJavaServiceIO && !myJavaServiceIO->isClosed())
    myJavaServiceIO->close();
}

int ServerHandler::connectImpl(std::function<void()> openBackwardTransport) {
  static int s_counter = 0;
  const int counter = s_counter++;
  setThreadName(string_format("ServerHandler_%d", counter));

  // Connect to client's side (for cef-callbacks execution on java side)
  try {
    openBackwardTransport();
    RemoteAppHandler::instance()->setService(myJavaService);
  } catch (TException& tx) {
    Log::error(tx.what());
    closeBackwardTransport();
    return -1;
  }

  return counter;
}

int32_t ServerHandler::connect(const std::string& backwardConnectionPipe, bool isMaster) {
  if (myJavaService != nullptr) {
    Log::error("Client already connected, other attempts will be ignored.");
    return -1;
  }

  myIsMaster = isMaster;

  return connectImpl([&](){
    myJavaService = std::make_shared<RpcExecutor>(backwardConnectionPipe);
    myJavaServiceIO = std::make_shared<RpcExecutor>(backwardConnectionPipe);
  });
}

int32_t ServerHandler::connectTcp(int backwardConnectionPort, bool isMaster) {
  if (myJavaService != nullptr) {
    Log::error("Client already connected (tcp), other attempts will be ignored.");
    return -1;
  }

  myIsMaster = isMaster;

  return connectImpl([&](){
    myJavaService = std::make_shared<RpcExecutor>(backwardConnectionPort);
    myJavaServiceIO = std::make_shared<RpcExecutor>(backwardConnectionPort);
  });
}

int32_t ServerHandler::createBrowser(int cid, int handlersMask) {
  int32_t bid = myClientsManager->createBrowser(cid, myJavaService, myJavaServiceIO, myRoutersManager, handlersMask);
  Log::trace("Created remote browser cid=%d, bid=%d, handlers: %s", cid, bid, HandlerMasks::toString(handlersMask).c_str());
  return bid;
}

void ServerHandler::startBrowserCreation(int bid, const std::string& url) {
  myClientsManager->startBrowserCreation(bid, url);
  Log::trace("Started creation of native CefBrowser of remote browser bid=%d", bid);
}

void ServerHandler::closeBrowser(const int32_t bid) {
  myClientsManager->closeBrowser(bid);
}

void ServerHandler::stop() {
  Log::debug("ServerHandler %p asked to stop server.", this);
  ServerState::instance().startShuttingDown();
  close();
}

void ServerHandler::state(std::string& _return) {
  _return = ServerState::instance().getStateDesc();
}

void ServerHandler::version(std::string& _return) {
  _return.assign(string_format("%d.%d.%d.%d",
    cef_version_info(0),   // CEF_VERSION_MAJOR
    cef_version_info(1),   // CEF_VERSION_MINOR
    cef_version_info(2),   // CEF_VERSION_PATCH
    cef_version_info(3)   // CEF_COMMIT_NUMBER
  ));
}

#define GET_BROWSER_OR_RETURN()                          \
  auto browser = myClientsManager->getCefBrowser(bid);   \
  if (browser == nullptr) {                              \
    Log::error("CefBrowser is null, bid=%d", bid);       \
    return;                                              \
  }

void ServerHandler::Browser_Reload(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->Reload();
}

void ServerHandler::Browser_ReloadIgnoreCache(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->ReloadIgnoreCache();
}

void ServerHandler::Browser_LoadURL(const int32_t bid, const std::string& url) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetMainFrame()->LoadURL(url);
}

void ServerHandler::Browser_GetURL(std::string& _return, const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  _return = browser->GetMainFrame()->GetURL().ToString();
}

void ServerHandler::Browser_ExecuteJavaScript(const int32_t bid,const std::string& code,const std::string& url,const int32_t line) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetMainFrame()->ExecuteJavaScript(code, url, line);
}

void ServerHandler::Browser_WasResized(const int32_t bid,const int32_t width,const int32_t height) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->WasResized();
}

extern void processKeyEvent(
    CefKeyEvent & cef_event,
    int event_type, // event.getID()
    int modifiers,  // event.getModifiersEx()
    char16_t key_char, // event.getKeyChar()
    long scanCode,   // event.scancode, windows only
    int key_code   // event.getKeyCode()
);

void ServerHandler::Browser_SendKeyEvent(const int32_t bid,const int32_t event_type,const int32_t modifiers,const int16_t key_char,const int64_t scanCode,const int32_t key_code) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  CefKeyEvent cef_event;
  processKeyEvent(cef_event, event_type, modifiers, key_char, scanCode, key_char);
  browser->GetHost()->SendKeyEvent(cef_event);
}

extern void processMouseEvent(
    CefRefPtr<CefBrowser> browser,
    int event_type, // getID
    int x, // getX
    int y, // getY
    int modifiers, // getModifiersEx
    int click_count, // getClickCount
    int button // getButton
);

void ServerHandler::Browser_SendMouseEvent(const int32_t bid,const int32_t event_type,const int32_t x,const int32_t y,const int32_t modifiers,const int32_t click_count,const int32_t button) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  processMouseEvent(browser, event_type, x, y, modifiers, click_count, button);
}

extern void processMouseWheelEvent(
    CefRefPtr<CefBrowser> browser,
    int scroll_type, // getScrollType
    int x, // getX
    int y, // getY
    int modifiers, // getModifiersEx
    int delta, // getWheelRotation
    int units_to_scroll // getUnitsToScroll
);

void ServerHandler::Browser_SendMouseWheelEvent(const int32_t bid,const int32_t scroll_type,const int32_t x,const int32_t y,const int32_t modifiers,const int32_t delta,const int32_t units_to_scroll) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  processMouseWheelEvent(browser, scroll_type, x, y, modifiers, delta, units_to_scroll);
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

  std::string result = rr->getDelegate().GetHeaderByName(name).ToString();
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

  rr->getDelegate().SetHeaderByName(name, value, overwrite);
}

void ServerHandler::Request_GetHeaderMap(
    std::map<std::string, std::string>& _return,
    const thrift_codegen::RObject& request) {
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr)
    return;

  CefRequest::HeaderMap hmap;
  rr->getDelegate().GetHeaderMap(hmap);
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
  rr->getDelegate().SetHeaderMap(hmap);
}

void ServerHandler::Response_GetHeaderByName(
    std::string& _return,
    const thrift_codegen::RObject& response,
    const std::string& name) {
  RemoteResponse * rr = RemoteResponse::get(response.objId);
  if (rr == nullptr)
    return;

  std::string result = rr->getDelegate().GetHeaderByName(name).ToString();
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

  rr->getDelegate().SetHeaderByName(name, value, overwrite);
}

void ServerHandler::Response_GetHeaderMap(
    std::map<std::string, std::string>& _return,
    const thrift_codegen::RObject& response) {
  RemoteResponse * rr = RemoteResponse::get(response.objId);
  if (rr == nullptr)
    return;

  CefRequest::HeaderMap hmap;
  rr->getDelegate().GetHeaderMap(hmap);
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
  rr->getDelegate().SetHeaderMap(hmap);
}

void ServerHandler::Request_GetPostData(
    thrift_codegen::PostData& _return,
    const thrift_codegen::RObject& request
) {
  _return.isReadOnly = true;
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr) return;

  CefRefPtr<CefPostData> pd = rr->getDelegate().GetPostData();
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
  rr->getDelegate().SetPostData(pd);
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
  rr->getDelegate().Set(url, method, pd, hmap);
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
  rc->getDelegate().Continue(username, password);
  RemoteAuthCallback::dispose(authCallback.objId);
}

void ServerHandler::AuthCallback_Cancel(const thrift_codegen::RObject& authCallback) {
  RemoteAuthCallback * rc = RemoteAuthCallback::get(authCallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate().Cancel();
  RemoteAuthCallback::dispose(authCallback.objId);
}

void ServerHandler::Callback_Dispose(const thrift_codegen::RObject& callback) {
  RemoteCallback::dispose(callback.objId);
}

void ServerHandler::Callback_Continue(const thrift_codegen::RObject& callback) {
  RemoteCallback * rc = RemoteCallback::get(callback.objId);
  if (rc == nullptr) return;
  rc->getDelegate().Continue();
  RemoteCallback::dispose(callback.objId);
}

void ServerHandler::Callback_Cancel(const thrift_codegen::RObject& callback) {
  RemoteCallback * rc = RemoteCallback::get(callback.objId);
  if (rc == nullptr) return;
  rc->getDelegate().Cancel();
  RemoteCallback::dispose(callback.objId);
}

void ServerHandler::MessageRouter_Create(thrift_codegen::RObject& _return,
                                        const std::string& query,
                                        const std::string& cancel) {
  _return = myRoutersManager->CreateRemoteMessageRouter(myJavaService, query, cancel)->serverId();
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
  // NOTE: must be called on UI thread (and [docs says] that CancelPending can be called on any thread)
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
      Log::error("Can't find router %d for removing handler %d", msgRouter.objId, handler.objId);
  }
}

void ServerHandler::MessageRouter_AddHandler(
    const thrift_codegen::RObject& msgRouter,
    const thrift_codegen::RObject& handler, bool first) {
  if (CefCurrentlyOn(TID_UI)) {
    ServerHandler_MessageRouter_AddHandler_Impl(myJavaService, myClientsManager, msgRouter, handler, first);
  } else {
    CefPostTask(TID_UI, base::BindOnce(
        [](std::shared_ptr<RpcExecutor> service,
           std::shared_ptr<ClientsManager> manager,
           const thrift_codegen::RObject& msgRouter,
           const thrift_codegen::RObject& handler,
           bool first) {
          ServerHandler_MessageRouter_AddHandler_Impl(service, manager, msgRouter, handler, first);
        },
            myJavaService, myClientsManager, msgRouter, handler, first));
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
  CefRefPtr<CefBrowser> browser = myClientsManager->getCefBrowser(bid);
  if (!rmr || !browser) return;
  std::shared_ptr<RemoteMessageRouterHandler> rmrh = rmr->FindRemoteHandler(handler.objId);
  if (rmrh)
    rmr->getDelegate().CancelPending(browser, rmrh.get());
}

void ServerHandler::QueryCallback_Dispose(const thrift_codegen::RObject& qcallback) {
  RemoteQueryCallback::dispose(qcallback.objId);
}

void ServerHandler::QueryCallback_Success(
    const thrift_codegen::RObject& qcallback,
    const std::string& response) {
  RemoteQueryCallback * rc = RemoteQueryCallback::get(qcallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate().Success(response);
  RemoteQueryCallback::dispose(qcallback.objId);
}

void ServerHandler::QueryCallback_Failure(
    const thrift_codegen::RObject& qcallback,
    const int32_t error_code,
    const std::string& error_message) {
  RemoteQueryCallback * rc = RemoteQueryCallback::get(qcallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate().Failure(error_code, error_message);
  RemoteQueryCallback::dispose(qcallback.objId);
}
