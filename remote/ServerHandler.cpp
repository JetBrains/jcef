#include "ServerHandler.h"

#include "include/cef_base.h"
#include "include/cef_parser.h"

#include "browser/ClientsManager.h"
#include "browser/RemoteDevToolsMessageObserver.h"
#include "browser/RemoteFrame.h"
#include "callback/RemoteMediaAccessCallback.h"
#include "handlers/RemoteClientHandler.h"
#include "handlers/app/RemoteAppHandler.h"
#include "network/RemoteCookieManager.h"
#include "network/RemoteCookieVisitor.h"
#include "network/RemotePostData.h"
#include "network/RemoteRequest.h"
#include "network/RemoteResponse.h"

#include "RemoteObjects.h"
#include "callback/RemoteAuthCallback.h"
#include "callback/RemoteCallback.h"
#include "callback/RemoteCefRunContextMenuCallback.h"
#include "callback/RemoteCompletionCallback.h"
#include "callback/RemoteIntCallback.h"
#include "callback/RemoteRegistration.h"
#include "callback/RemoteSchemeHandlerFactory.h"
#include "callback/RemoteStringVisitor.h"
#include "callback/RemoteRunFileDialogCallback.h"
#include "callback/RemotePdfPrintCallback.h"

#include "include/base/cef_callback.h"
#include "include/wrapper/cef_closure_task.h"
#include "router/MessageRoutersManager.h"
#include "router/RemoteMessageRouter.h"
#include "router/RemoteMessageRouterHandler.h"
#include "router/RemoteQueryCallback.h"

#include "ServerApplication.h"
#include "ServerHandlerContext.h"

#include "../native/critical_wait.h"
#include "CefUtils.h"

using namespace apache::thrift;

namespace {
  void executeDevToolsMethod(CefRefPtr<CefBrowserHost> host,
                             const CefString& method,
                             const CefString& parametersAsJson,
                             CefRefPtr<RemoteIntCallback> callback
  ) {
    CefRefPtr<CefDictionaryValue> parameters = nullptr;
    if (!parametersAsJson.empty()) {
      CefRefPtr<CefValue> value = CefParseJSON(
          parametersAsJson, cef_json_parser_options_t::JSON_PARSER_RFC);

      if (!value || value->GetType() != VTYPE_DICTIONARY) {
        callback->OnComplete(0);
        return;
      }

      parameters = value->GetDictionary();
    }

    callback->OnComplete(host->ExecuteDevToolsMethod(0, method, parameters));
  }
}

ServerHandler::ServerHandler() : myCtx(nullptr) {}

ServerHandler::~ServerHandler() {
  close();
  if (myCtx)
    myCtx->closeJavaServiceTransport();
}

void ServerHandler::close() {
  if (myIsClosed)
    return;

  myIsClosed = true;
  ServerApplication::instance().onServerHandlerClosed(*this);
  if (myCtx)
    myCtx->close();
}

int ServerHandler::connectImpl(std::function<void()> openBackwardTransport) {
  if (myCtx != nullptr) {
    Log::error("Client already connected, other attempts will be ignored.");
    return -1;
  }

  static int s_counter = 0;
  const int counter = s_counter++;
  setThreadName(string_format("ServerHandler_%d", counter));

  // Connect to client's side (for cef-callbacks execution on java side)
  myCtx = std::make_shared<ServerHandlerContext>();
  try {
    openBackwardTransport();
    static const bool testBackwardTransport = getBoolEnv("CEF_SERVER_TEST_BACKWARD_TRANSPORT");
    if (testBackwardTransport) {
      // simple test for connection
      std::string testMsg = "123test!!";
      std::string returnVal;
      myCtx->javaService()->exec(
          [&](JavaService s) { s->echo(returnVal, testMsg); });
      if (testMsg.compare(returnVal) != 0) {
        Log::error("JavaClient returns invalid echo '%s'", returnVal.c_str());
        myCtx->closeJavaServiceTransport();
        return -1;
      }
    }
    ServerApplication::instance().getCefAppHandler()->setService(myCtx->javaService());
  } catch (TException& tx) {
    Log::error(tx.what());
    myCtx->closeJavaServiceTransport();
    return -1;
  }

  return myCid = counter;
}

int32_t ServerHandler::connect(const std::string& backwardConnectionPipe, bool isMaster) {
  myIsMaster = isMaster;

  return connectImpl([&](){
    myCtx->initJavaServicePipe(backwardConnectionPipe);
  });
}

int32_t ServerHandler::connectTcp(int backwardConnectionPort, bool isMaster) {
  myIsMaster = isMaster;

  return connectImpl([&](){
    myCtx->initJavaServicePort(backwardConnectionPort);
  });
}

void ServerHandler::attach(int cid) {
  myCtx = ServerApplication::instance().getCtx(cid);
}

int32_t ServerHandler::Browser_Create(int cid, int handlersMask, const thrift_codegen::RObject& requestContextHandler) {
  int32_t bid = myCtx->clientsManager()->createBrowser(cid, myCtx, handlersMask, requestContextHandler);
  if (Log::isTraceEnabled()) {
    std::string hdesc = "";
    if (!requestContextHandler.isNull)
      hdesc = string_format(" [request context handler %d]", requestContextHandler.objId);
    Log::trace("Created remote browser cid=%d, bid=%d%s, handlers: %s", cid, bid, hdesc.c_str(), HandlerMasks::toString(handlersMask).c_str());
  }
  return bid;
}

void ServerHandler::Browser_StartNativeCreation(int bid, const std::string& url) {
  myCtx->clientsManager()->startNativeBrowserCreation(bid, url);
  Log::trace("Started creation of native CefBrowser of remote browser bid=%d, url=%s", bid, url.c_str());
}

void ServerHandler::Browser_OpenDevTools(int bid, int x, int y) {
  myCtx->clientsManager()->openDevTools(bid, x, y);
  Log::trace("Started opening of dev-tools of remote browser bid=%d", bid);
}

void ServerHandler::Browser_Close(const int32_t bid) {
  myCtx->clientsManager()->closeBrowser(bid);
}

void ServerHandler::stop() {
  Log::debug("ServerHandler %p asked to stop server.", this);
  ServerApplication::instance().startShuttingDown();
  close();
}

void ServerHandler::getServerInfo(std::string& _return, const std::string& request) {
  if (request.compare("version") == 0)
    _return.assign(CefUtils::getVersionWithSha());
  else if (request.compare("state") == 0)
    _return = ServerApplication::instance().getStateDesc();
  else if (request.compare("root") == 0)
    _return = ServerApplication::instance().getCefAppHandler()->getRootPath();
  else
    _return = "Unknown request: " + request;
}

#define GET_BROWSER_OR_RETURN()                               \
  auto browser = myCtx->clientsManager()->getCefBrowser(bid); \
  if (browser == nullptr) {                                   \
    Log::error("CefBrowser is null, bid=%d", bid);            \
    return;                                                   \
  }

#define GET_BROWSER_OR_RETURN_VAL(val)                        \
  auto browser = myCtx->clientsManager()->getCefBrowser(bid); \
  if (browser == nullptr) {                                   \
    Log::error("CefBrowser is null, bid=%d", bid);            \
    return val;                                               \
  }

#define GET_CLIENT_OR_RETURN()                              \
  auto client = myCtx->clientsManager()->getClient(bid);    \
  if (client == nullptr) {                                  \
    Log::error("RemoteClientHandler is null, bid=%d", bid); \
    return;                                                 \
  }

#define GET_CLIENT_OR_RETURN_VAL(val)                       \
  auto client = myCtx->clientsManager()->getClient(bid);    \
  if (client == nullptr) {                                  \
    Log::error("RemoteClientHandler is null, bid=%d", bid); \
    return val;                                             \
  }

#define GET_COOKIE_MANAGER_OR_RETURN()                                            \
  RemoteCookieManager * manager = RemoteCookieManager::find(cookieManager.objId); \
  if (manager == nullptr) {                                                       \
    Log::error("Can't find RemoteCookieManager by id=%d", cookieManager.objId);   \
    return;                                                                       \
  }

#define GET_COOKIE_MANAGER_OR_RETURN_VAL(val)                                     \
  RemoteCookieManager * manager = RemoteCookieManager::find(cookieManager.objId); \
  if (manager == nullptr) {                                                       \
    Log::error("Can't find RemoteCookieManager by id=%d", cookieManager.objId);   \
    return val;                                                                       \
  }

void ServerHandler::Browser_CloseDevTools(const int32_t bid) {
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->CloseDevTools();
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
  Log::trace("Browser %d is loading URL '%s'", bid, url.c_str());
  browser->GetMainFrame()->LoadURL(url);
}

void ServerHandler::Browser_LoadRequest(const int32_t bid, const thrift_codegen::RObject & request) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr) {
    Log::trace("Can't find RemoteRequest %d", request.objId);
    return;
  }
  Log::trace("Browser %d is loading request %d", bid, request.objId);
  CefRefPtr<CefRequest> r(&(rr->getDelegate()));
  browser->GetMainFrame()->LoadRequest(r);
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

void ServerHandler::Frame_ExecuteJavaScript(const int32_t frameId, const std::string& code, const std::string& url, const int32_t line) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf != nullptr)
    rf->getDelegate().ExecuteJavaScript(code, url, line);
}

void ServerHandler::Frame_Dispose(const int32_t frameId) {
  LNDCT();
  RemoteFrame::dispose(frameId);
}

void ServerHandler::Frame_GetParent(thrift_codegen::RObject & _return, int frameId) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf == nullptr)
    return;

  RemoteFrame* rparent = RemoteFrame::create(rf->getDelegate().GetParent());
  if (rparent != nullptr)
    _return = rparent->serverId();
}

void ServerHandler::Frame_Undo(int frameId) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf != nullptr)
    rf->getDelegate().Undo();
}

void ServerHandler::Frame_Redo(int frameId) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf != nullptr)
    rf->getDelegate().Redo();
}

void ServerHandler::Frame_Cut(int frameId) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf != nullptr)
    rf->getDelegate().Cut();
}

void ServerHandler::Frame_Copy(int frameId) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf != nullptr)
    rf->getDelegate().Copy();
}

void ServerHandler::Frame_Paste(int frameId) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf != nullptr)
    rf->getDelegate().Paste();
}

void ServerHandler::Frame_Delete(int frameId) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf != nullptr)
    rf->getDelegate().Delete();
}

void ServerHandler::Frame_SelectAll(int frameId) {
  LNDCT();
  RemoteFrame * rf = RemoteFrame::get(frameId);
  if (rf != nullptr)
    rf->getDelegate().SelectAll();
}

void ServerHandler::Browser_WasResized(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->WasResized();
}

void ServerHandler::Browser_NotifyScreenInfoChanged(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->NotifyScreenInfoChanged();
}

void ServerHandler::Browser_Invalidate(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->Invalidate(PET_VIEW);;
}

extern void processKeyEvent(
    CefKeyEvent & cef_event,
    int event_type, // event.getID()
    int modifiers,  // event.getModifiersEx()
    char16_t key_char, // event.getKeyChar()
    long scanCode,   // event.scancode, windows only
    int key_code   // event.getKeyCode()
);

void ServerHandler::Browser_SendCefKeyEvent(
    const int32_t bid,
    const thrift_codegen::CefKeyEventAttributes& event) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  CefKeyEvent cef_event{};
  cef_event.type = static_cast<cef_key_event_type_t>(event.type);
  cef_event.modifiers = event.modifiers;
  cef_event.windows_key_code = event.windows_key_code;
  cef_event.native_key_code = event.native_key_code;
  cef_event.is_system_key = event.is_system_key;
  cef_event.character = event.character;
  cef_event.unmodified_character = event.unmodified_character;

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

void ServerHandler::Browser_GoBack(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GoBack();
}

bool ServerHandler::Browser_CanGoForward(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN_VAL(false)
  return browser->CanGoForward();
}

bool ServerHandler::Browser_CanGoBack(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN_VAL(false)
  return browser->CanGoBack();
}

void ServerHandler::Browser_GoForward(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GoForward();
}

bool ServerHandler::Browser_IsLoading(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN_VAL(false)
  return browser->IsLoading();
}
void ServerHandler::Browser_StopLoad(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->StopLoad();
}

void ServerHandler::Browser_GetMainFrame(thrift_codegen::RObject& _return, const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  CefRefPtr<CefFrame> frame = browser->GetMainFrame();
  RemoteFrame* rf = RemoteFrame::create(frame);
  if (rf != nullptr)
    _return = rf->serverId();
}

void ServerHandler::Browser_GetFocusedFrame(thrift_codegen::RObject& _return, const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  CefRefPtr<CefFrame> frame = browser->GetFocusedFrame();
  RemoteFrame* rf = RemoteFrame::create(frame);
  if (rf != nullptr)
    _return = rf->serverId();
}

void ServerHandler::Browser_GetFrameByIdentifier(thrift_codegen::RObject& _return, const int32_t bid, const std::string& id) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  CefRefPtr<CefFrame> frame = browser->GetFrameByIdentifier(id);
  RemoteFrame* rf = RemoteFrame::create(frame);
  if (rf != nullptr)
    _return = rf->serverId();
}

void ServerHandler::Browser_GetFrameByName(thrift_codegen::RObject& _return, const int32_t bid, const std::string& name) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  CefRefPtr<CefFrame> frame = browser->GetFrameByName(name);
  RemoteFrame* rf = RemoteFrame::create(frame);
  if (rf != nullptr)
    _return = rf->serverId();
}

void ServerHandler::Browser_GetFrameIdentifiers(std::vector<std::string>& _return, const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  std::vector<CefString> identifiers;
  browser->GetFrameIdentifiers(identifiers);
  for (const auto & fid: identifiers)
    _return.push_back(fid.ToString());
}

void ServerHandler::Browser_GetFrameNames(std::vector<std::string>& _return, const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  std::vector<CefString> names;
  browser->GetFrameNames(names);
  for (const auto & name : names)
    _return.push_back(name.ToString());
}

int32_t ServerHandler::Browser_GetFrameCount(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN_VAL(0)
  return (int32_t)browser->GetFrameCount();
}

bool ServerHandler::Browser_IsPopup(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN_VAL(false)
  return browser->IsPopup();
}

bool ServerHandler::Browser_HasDocument(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN_VAL(false)
  return browser->HasDocument();
}

void ServerHandler::Browser_ViewSource(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  CefRefPtr<CefFrame> mainFrame = browser->GetMainFrame();
  CefPostTask(TID_UI, base::BindOnce(&CefFrame::ViewSource, mainFrame.get()));
}

void ServerHandler::Browser_GetSource(const int32_t bid, const thrift_codegen::RObject& stringVisitor) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetMainFrame()->GetSource(new RemoteStringVisitor(myCtx, stringVisitor));
}

void ServerHandler::Browser_GetText(const int32_t bid, const thrift_codegen::RObject& stringVisitor) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetMainFrame()->GetText(new RemoteStringVisitor(myCtx, stringVisitor));
}

void ServerHandler::Browser_SetFocus(const int32_t bid, bool enable) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->SetFocus(enable);
}

namespace {
  void _runTaskAndWakeup(std::shared_ptr<CriticalWait> waitCond,
                         base::OnceClosure task) {
    WaitGuard guard(*waitCond);
    std::move(task).Run();
    waitCond->WakeUp();
  }

  void CefPostTaskAndWait(CefThreadId threadId,
                          base::OnceClosure task,
                          long waitMillis) {
    std::shared_ptr<CriticalLock> lock = std::make_shared<CriticalLock>();
    std::shared_ptr<CriticalWait> waitCond = std::make_shared<CriticalWait>(lock.get());
    LockGuard guard(*lock);
    CefPostTask(threadId, base::BindOnce(_runTaskAndWakeup, waitCond, std::move(task)));
    waitCond->Wait(static_cast<unsigned>(waitMillis));
  }

  void getZoomLevel(CefRefPtr<CefBrowserHost> host, std::shared_ptr<double> result) {
    *result = host->GetZoomLevel();
  }
}

double ServerHandler::Browser_GetZoomLevel(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN_VAL(0.0f)

  CefRefPtr<CefBrowserHost> host = browser->GetHost();
  if (CefCurrentlyOn(TID_UI)) {
    return host->GetZoomLevel();
  }
  std::shared_ptr<double> result = std::make_shared<double>(0.0);
  CefPostTaskAndWait(TID_UI, base::BindOnce(getZoomLevel, host, result), 100);
  return *result;
}

void ServerHandler::Browser_SetZoomLevel(const int32_t bid, const double val)   {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->SetZoomLevel(val);
}

void ServerHandler::Browser_StartDownload(const int32_t bid, const std::string& url) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->StartDownload(url);
}

void ServerHandler::Browser_Find(const int32_t bid, const std::string& searchText, const bool forward, const bool matchCase, const bool findNext)   {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->Find(searchText, forward, matchCase, findNext);
}

void ServerHandler::Browser_StopFinding(const int32_t bid, const bool clearSelection)   {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->StopFinding(clearSelection);
}

void ServerHandler::Browser_ReplaceMisspelling(const int32_t bid, const std::string& word)   {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->ReplaceMisspelling(word);
}

void ServerHandler::Browser_SetFrameRate(const int32_t bid, int32_t val) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->SetWindowlessFrameRate(val);
}

void ServerHandler::Browser_AddDevToolsMessageObserver(thrift_codegen::RObject& _return, const int32_t bid, const thrift_codegen::RObject& observer) {
  LNDCT();
  GET_BROWSER_OR_RETURN()

  CefRefPtr<RemoteDevToolsMessageObserver> robserver(new RemoteDevToolsMessageObserver(myCtx->clientsManager(), myCtx, observer));
  CefRefPtr<CefRegistration> registration = browser->GetHost()->AddDevToolsMessageObserver(robserver);
  _return = RemoteRegistration::create(registration)->serverId();
}

void ServerHandler::Browser_ExecuteDevToolsMethod(
    const int32_t bid,
    const std::string& method,
    const std::string& parametersAsJson,
    const thrift_codegen::RObject& intCallback
) {
  LNDCT();
  GET_BROWSER_OR_RETURN()

  CefRefPtr<RemoteIntCallback> callback = new RemoteIntCallback(myCtx, intCallback);
  if (!browser.get()) {
    callback->OnComplete(0);
    return;
  }

  if (CefCurrentlyOn(TID_UI)) {
    executeDevToolsMethod(browser->GetHost(), method, parametersAsJson, callback);
  } else {
    CefPostTask(TID_UI,
                base::BindOnce(executeDevToolsMethod,
                               browser->GetHost(), method, parametersAsJson, callback));
  }
}

void ServerHandler::Browser_RunFileDialog(
    const int32_t bid,
    const std::string& mode,
    const std::string& title,
    const std::string& defaultFilePath,
    const std::vector<std::string>& acceptFilters,
    const thrift_codegen::RObject& runFileDialogCallback) {
  LNDCT();
  GET_BROWSER_OR_RETURN()

  CefRefPtr<RemoteRunFileDialogCallback> callback = new RemoteRunFileDialogCallback(myCtx, runFileDialogCallback);
  if (!browser.get()) {
    callback->OnFileDialogDismissed(std::vector<CefString>());
    return;
  }

  std::vector<CefString> accept_types;
  for (const auto & fp: acceptFilters)
    accept_types.push_back(CefString(fp));

  CefBrowserHost::FileDialogMode fdMode;
  if (mode.find("FILE_DIALOG_OPEN_MULTIPLE") != std::string::npos)
    fdMode = FILE_DIALOG_OPEN_MULTIPLE;
  else if (mode.find("FILE_DIALOG_OPEN") != std::string::npos)
    fdMode = FILE_DIALOG_OPEN;
  else if (mode.find("FILE_DIALOG_SAVE") != std::string::npos)
    fdMode = FILE_DIALOG_SAVE;
  else
    fdMode = FILE_DIALOG_OPEN;

  browser->GetHost()->RunFileDialog(fdMode, CefString(title), CefString(defaultFilePath), accept_types, callback);
}

namespace {
void setFieldValueD(double & field, const std::string & val) {
  try {
    field = stod(val);
  } catch (const std::logic_error & ex) {
    Log::error("PrintToPDF: can't convert string '%s' to double. Error: %s", val.c_str(), ex.what());
    field = 1.f;
  }
}
void setFieldValueB(int & field, const std::string & val) {
  field = (val.compare("true") == 0);
}
}

void ServerHandler::Browser_PrintToPDF(
    const int32_t bid,
    const std::string& path,
    const std::map<std::string, std::string>& pdfPrintSettings,
    const thrift_codegen::RObject& pdfPrintCallback) {
  LNDCT();
  GET_BROWSER_OR_RETURN()

  CefRefPtr<RemotePdfPrintCallback> callback = new RemotePdfPrintCallback(myCtx, pdfPrintCallback);
  if (!browser.get()) {
    callback->OnPdfPrintFinished(CefString(path), false);
    return;
  }

  CefPdfPrintSettings settings;
  for (const auto & kv: pdfPrintSettings) {
    if (kv.first.compare("landscape") == 0)
      setFieldValueB(settings.landscape, kv.second);
    else if (kv.first.compare("print_background") == 0)
      setFieldValueB(settings.print_background, kv.second);
    else if (kv.first.compare("scale") == 0)
      setFieldValueD(settings.scale, kv.second);
    else if (kv.first.compare("paper_width") == 0)
      setFieldValueD(settings.paper_width, kv.second);
    else if (kv.first.compare("paper_height") == 0)
      setFieldValueD(settings.paper_height, kv.second);
    else if (kv.first.compare("prefer_css_page_size") == 0)
      setFieldValueB(settings.prefer_css_page_size, kv.second);
    else if (kv.first.compare("margin_type") == 0) {
      settings.margin_type = PDF_PRINT_MARGIN_DEFAULT;
      if (kv.second.find("NONE") != std::string::npos)
        settings.margin_type = PDF_PRINT_MARGIN_NONE;
      else if (kv.second.find("CUSTOM") != std::string::npos)
        settings.margin_type = PDF_PRINT_MARGIN_CUSTOM;
    } else if (kv.first.compare("margin_top") == 0)
      setFieldValueD(settings.margin_top, kv.second);
    else if (kv.first.compare("margin_bottom") == 0)
      setFieldValueD(settings.margin_bottom, kv.second);
    else if (kv.first.compare("margin_right") == 0)
      setFieldValueD(settings.margin_right, kv.second);
    else if (kv.first.compare("margin_left") == 0)
      setFieldValueD(settings.margin_left, kv.second);
    else if (kv.first.compare("page_ranges") == 0) {
      std::string ranges = kv.second;
      CefString(&settings.page_ranges) = ranges;
    } else if (kv.first.compare("display_header_footer") == 0)
      setFieldValueB(settings.display_header_footer, kv.second);
    else if (kv.first.compare("header_template") == 0) {
      std::string templ = kv.second;
      CefString(&settings.header_template) = templ;
    } else if (kv.first.compare("footer_template") == 0) {
      std::string templ = kv.second;
      CefString(&settings.footer_template) = templ;
    }  else if (kv.first.compare("generate_document_outline") == 0) {
      setFieldValueB(settings.generate_document_outline, kv.second);
    }  else if (kv.first.compare("generate_tagged_pdf") == 0) {
      setFieldValueB(settings.generate_tagged_pdf, kv.second);
    }
  }

  browser->GetHost()->PrintToPDF(path, settings, callback);
}

void ServerHandler::Browser_Print(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->Print();
}

void ServerHandler::Browser_ImeSetComposition(
    const int32_t bid,
    const std::string& text,
    const std::vector<thrift_codegen::CompositionUnderline>& underlines,
    const thrift_codegen::Range& replacementRange,
    const thrift_codegen::Range& selectionRange) {
  LNDCT();
  GET_BROWSER_OR_RETURN()

  std::vector<CefCompositionUnderline> cef_underlines;
  for (const auto & underline: underlines) {
    CefCompositionUnderline cef_underline;
    cef_underline.range = CefRange(underline.range.from, underline.range.to);
    cef_underline.thick = underline.thick;

    cef_underline.color = CefColorSetARGB(
      underline.color.alpha, underline.color.red,
      underline.color.green, underline.color.blue
    );

    cef_underline.background_color = CefColorSetARGB(
      underline.backgroundColor.alpha, underline.backgroundColor.red,
      underline.backgroundColor.green, underline.backgroundColor.blue
    );

    cef_underlines.push_back(cef_underline);
  }

  browser->GetHost()->ImeSetComposition(
      CefString(text),
      cef_underlines,
      CefRange(replacementRange.from, replacementRange.to),
      CefRange(selectionRange.from, selectionRange.to)
    );
}


void ServerHandler::Browser_ImeCommitText(
    const int32_t bid,
    const std::string& text,
    const thrift_codegen::Range& replacementRange,
    const int32_t relativeCursorPos) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->ImeCommitText(
      CefString(text),
      CefRange(replacementRange.from, replacementRange.to),
      relativeCursorPos
    );
}

void ServerHandler::Browser_ImeFinishComposingText(const int32_t bid, const bool keepSelection) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->ImeFinishComposingText(keepSelection);
}

void ServerHandler::Browser_ImeCancelComposing(const int32_t bid) {
  LNDCT();
  GET_BROWSER_OR_RETURN()
  browser->GetHost()->ImeCancelComposition();
}

void ServerHandler::Request_Create(thrift_codegen::RObject& result) {
  CefRefPtr<CefRequest> request = CefRequest::Create();
  RemoteRequest* rr = RemoteRequest::create(request);
  if (rr != nullptr) {
    result = rr->serverId();
    result.isNull = false;
  }
}

void ServerHandler::Request_Dispose(int requestId) {
  LNDCT();
  RemoteRequest::dispose(requestId);
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
  RemoteRequest * rr = RemoteRequest::get(request.objId);
  if (rr == nullptr) return;

  CefRefPtr<CefPostData> pd = rr->getDelegate().GetPostData();
  if (!pd) return;

  _return.isNull = false;
  _return.isReadOnly = pd->IsReadOnly();
  _return.hasExcludedElements = pd->HasExcludedElements();
  if (pd->GetElementCount() > 0) {
    std::vector<thrift_codegen::PostDataElement> resultElements;
    CefPostData::ElementVector elements;
    pd->GetElements(elements);
    for (auto e : elements) {
      thrift_codegen::PostDataElement ee;
      ee.isReadOnly = e->IsReadOnly();
      ee.type = e->GetType();
      if (e->GetType() == PDE_TYPE_FILE)
        ee.__set_file(e->GetFile());
      if (e->GetBytesCount() > 0) {
        char* buf = new char[e->GetBytesCount()];
        e->GetBytes(e->GetBytesCount(), buf);
        std::string bytes((const char*)buf, e->GetBytesCount());
        bytes.assign((const char*)buf, e->GetBytesCount());
        ee.__set_bytes(bytes);
        delete[] buf;
      }
      resultElements.push_back(ee);
    }
    _return.__set_elements(resultElements);
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

void ServerHandler::CefRunContextMenuCallback_Dispose(
    const thrift_codegen::RObject& self) {
  RemoteCefRunContextMenuCallback::dispose(self.objId);
}

void ServerHandler::CefRunContextMenuCallback_Continue(
    const thrift_codegen::RObject& self,
    const int32_t command_id,
    const int32_t event_flag) {
  const auto self_wrapper = RemoteCefRunContextMenuCallback::get(self.objId);
  if (self_wrapper == nullptr) return;
  self_wrapper->getDelegate().Continue(command_id,
                              static_cast<cef_event_flags_t>(event_flag));
  RemoteCefRunContextMenuCallback::dispose(self.objId);
}

void ServerHandler::CefRunContextMenuCallback_Cancel(
    const thrift_codegen::RObject& self) {
  const auto self_wrapper = RemoteCefRunContextMenuCallback::get(self.objId);
  if (self_wrapper == nullptr)
    return;
  self_wrapper->getDelegate().Cancel();
  RemoteCefRunContextMenuCallback::dispose(self.objId);
}

void ServerHandler::MessageRouter_Create(thrift_codegen::RObject& _return,
                                        const std::string& query,
                                        const std::string& cancel) {
  _return = myCtx->routersManager()->CreateRemoteMessageRouter(myCtx, query, cancel)->serverId();
}

void ServerHandler::MessageRouter_Dispose(const thrift_codegen::RObject& msgRouter) {
  myCtx->routersManager()->DisposeRemoteMessageRouter(msgRouter.objId);
}

void ServerHandler::MessageRouter_AddMessageRouterToBrowser(
    const thrift_codegen::RObject& msgRouter,
    const int32_t bid) {
  LNDCT();
  RemoteMessageRouter * rmr = RemoteMessageRouter::get(msgRouter.objId);
  if (rmr == nullptr) return;

  // Update running render-processes.
  CefRefPtr<CefBrowser> browser = myCtx->clientsManager()->getCefBrowser(bid);
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
  CefRefPtr<CefBrowser> browser = myCtx->clientsManager()->getCefBrowser(bid);
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
    ServerHandler_MessageRouter_AddHandler_Impl(myCtx->javaService(), myCtx->clientsManager(), msgRouter, handler, first);
  } else {
    CefPostTask(TID_UI, base::BindOnce(
        [](std::shared_ptr<RpcExecutor> service,
           std::shared_ptr<ClientsManager> manager,
           const thrift_codegen::RObject& msgRouter,
           const thrift_codegen::RObject& handler,
           bool first) {
          ServerHandler_MessageRouter_AddHandler_Impl(service, manager, msgRouter, handler, first);
        },
            myCtx->javaService(), myCtx->clientsManager(), msgRouter, handler, first));
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
  CefRefPtr<CefBrowser> browser = myCtx->clientsManager()->getCefBrowser(bid);
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

///
/// Register a scheme handler factory with the global request context. An empty
/// |domain_name| value for a standard scheme will cause the factory to match
/// all domain names. The |domain_name| value will be ignored for non-standard
/// schemes. If |scheme_name| is a built-in scheme and no handler is returned by
/// |factory| then the built-in scheme handler factory will be called. If
/// |scheme_name| is a custom scheme then you must also implement the
/// CefApp::OnRegisterCustomSchemes() method in all processes. This function may
/// be called multiple times to change or remove the factory that matches the
/// specified |scheme_name| and optional |domain_name|. Returns false if an
/// error occurs. This function may be called on any thread in the browser
/// process. Using this function is equivalent to calling
/// CefRequestContext::GetGlobalContext()->RegisterSchemeHandlerFactory().
///
void ServerHandler::SchemeHandlerFactory_Register(
    const std::string& schemeName,
    const std::string& domainName,
    const thrift_codegen::RObject& schemeHandlerFactory) {
  CefRefPtr<RemoteSchemeHandlerFactory> factory = new RemoteSchemeHandlerFactory(myCtx->clientsManager(), myCtx, schemeHandlerFactory);
  const bool result = CefRegisterSchemeHandlerFactory(schemeName,domainName, factory);
  if (result)
    Log::trace("Registered SchemeHandlerFactory: schemeName=%s, domainName=%s, peer-id=%d", schemeName.c_str(), domainName.c_str(), schemeHandlerFactory.objId);
  else
    Log::error("Can't register SchemeHandlerFactory: schemeName=%s, domainName=%s, peer-id=%d", schemeName.c_str(), domainName.c_str(), schemeHandlerFactory.objId);
}

void ServerHandler::ClearAllSchemeHandlerFactories() {
  Log::trace("Cleared all SchemeHandlerFactory instances.");
  CefClearSchemeHandlerFactories();
}

void ServerHandler::RequestContext_ClearCertificateExceptions(const int32_t bid, const thrift_codegen::RObject& rcompletionCallback) {
  LNDCT();
  CefRefPtr<RemoteCompletionCallback> cb;
  if (!rcompletionCallback.isNull)
    cb = new RemoteCompletionCallback(myCtx, rcompletionCallback);
  if (bid < 0) {
    // NOTE: assume that GlobalContext is linked with negative bid.
    CefRefPtr<CefRequestContext> globalContext = CefRequestContext::GetGlobalContext();
    if (globalContext) {
      globalContext->ClearCertificateExceptions(cb);
      Log::debug("Cleared all certificate exceptions in global RequestContext.");
    }
    return;
  }
  GET_CLIENT_OR_RETURN()
  client->getRequestContext()->ClearCertificateExceptions(cb);
}

void ServerHandler::RequestContext_CloseAllConnections(const int32_t bid, const thrift_codegen::RObject& rcompletionCallback) {
  LNDCT();
  CefRefPtr<RemoteCompletionCallback> cb;
  if (!rcompletionCallback.isNull)
    cb = new RemoteCompletionCallback(myCtx, rcompletionCallback);
  if (bid < 0) {
    // NOTE: assume that GlobalContext is linked with negative bid.
    CefRefPtr<CefRequestContext> globalContext = CefRequestContext::GetGlobalContext();
    if (globalContext) {
      globalContext->CloseAllConnections(cb);
      Log::debug("Closed all connections in global RequestContext.");
    }
    return;
  }
  GET_CLIENT_OR_RETURN()
  client->getRequestContext()->CloseAllConnections(cb);
}

void ServerHandler::CookieManager_Create(thrift_codegen::RObject& _return) {
  // TODO(JCEF): Expose the callback object.
  CefRefPtr<CefCookieManager> manager = CefCookieManager::GetGlobalManager(nullptr);
  if (!manager)
    return;

  RemoteCookieManager * rm = RemoteCookieManager::create(myCtx->javaServiceIO(), manager);
  _return = rm->serverId();
}

void ServerHandler::CookieManager_Dispose(const thrift_codegen::RObject& cookieManager) {
  RemoteCookieManager::dispose(cookieManager.objId);
}

bool ServerHandler::CookieManager_VisitAllCookies(const thrift_codegen::RObject& cookieManager, const thrift_codegen::RObject& visitor) {
  GET_COOKIE_MANAGER_OR_RETURN_VAL(false);
  CefRefPtr<RemoteCookieVisitor> rvisitor(new RemoteCookieVisitor(myCtx, visitor));
  return manager->getDelegate().VisitAllCookies(rvisitor);
}

bool ServerHandler::CookieManager_VisitUrlCookies(
    const thrift_codegen::RObject& cookieManager,
    const thrift_codegen::RObject& visitor,
    const std::string& url,
    const bool includeHttpOnly
) {
  GET_COOKIE_MANAGER_OR_RETURN_VAL(false);
  CefRefPtr<RemoteCookieVisitor> rvisitor(new RemoteCookieVisitor(myCtx, visitor));
  return manager->getDelegate().VisitUrlCookies(url, includeHttpOnly, rvisitor);
}

bool ServerHandler::CookieManager_SetCookie(
    const thrift_codegen::RObject& cookieManager,
    const std::string& url,
    const thrift_codegen::Cookie& c
) {
  GET_COOKIE_MANAGER_OR_RETURN_VAL(false);
  CefCookie cookie;
  RemoteCookieVisitor::toCefCookie(c, cookie);

  // The method CefCookieManager::SetCookie must be called on the IO thread.
  // We ignore its return value and return the result of the PostTask event to
  // java instead.
  // TODO(JCEF): Expose the callback object.
  CefRefPtr<CefCookieManager> pmanager(&(manager->getDelegate()));
  bool result = CefPostTask(
      TID_IO, base::BindOnce(base::IgnoreResult(&CefCookieManager::SetCookie),
                             pmanager, url, cookie,
                             CefRefPtr<CefSetCookieCallback>()));
  return result;
}

bool ServerHandler::CookieManager_DeleteCookies(
    const thrift_codegen::RObject& cookieManager,
    const std::string& url,
    const std::string& cookieName
) {
  GET_COOKIE_MANAGER_OR_RETURN_VAL(false);

  // The method CefCookieManager::DeleteCookies must be called on the IO thread.
  // We ignore its return value and return the result of the PostTask event to
  // java instead.
  // TODO(JCEF): Expose the callback object.
  CefRefPtr<CefCookieManager> pmanager(&(manager->getDelegate()));
  bool result = CefPostTask(
      TID_IO, base::BindOnce(base::IgnoreResult(&CefCookieManager::DeleteCookies),
                             pmanager, url, cookieName,
                             CefRefPtr<CefDeleteCookiesCallback>()));
  return result;
}

bool ServerHandler::CookieManager_FlushStore(
    const thrift_codegen::RObject& cookieManager,
    const thrift_codegen::RObject& rcompletionCallback
) {
  GET_COOKIE_MANAGER_OR_RETURN_VAL(false);

  CefRefPtr<RemoteCompletionCallback> cb;
  if (!rcompletionCallback.isNull)
    cb = new RemoteCompletionCallback(myCtx, rcompletionCallback);
  return manager->getDelegate().FlushStore(cb);
}

void ServerHandler::Registration_Dispose(const thrift_codegen::RObject& registration) {
  RemoteRegistration::dispose(registration.objId);
}

void ServerHandler::MediaAccessCallback_Dispose(const thrift_codegen::RObject& mediaAccessCallback) {
  RemoteMediaAccessCallback::dispose(mediaAccessCallback.objId);
}

void ServerHandler::MediaAccessCallback_Continue(const thrift_codegen::RObject& mediaAccessCallback, const int32_t allowed_permissions) {
  RemoteMediaAccessCallback * rc = RemoteMediaAccessCallback::get(mediaAccessCallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate().Continue(allowed_permissions);
  RemoteMediaAccessCallback::dispose(mediaAccessCallback.objId);
}

void ServerHandler::MediaAccessCallback_Cancel(const thrift_codegen::RObject& mediaAccessCallback) {
  RemoteMediaAccessCallback * rc = RemoteMediaAccessCallback::get(mediaAccessCallback.objId);
  if (rc == nullptr) return;
  rc->getDelegate().Cancel();
  RemoteMediaAccessCallback::dispose(mediaAccessCallback.objId);
}
