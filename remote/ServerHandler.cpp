#include "ServerHandler.h"

#include <log4cxx/mdc.h>
#include <thread>

#include "CefBrowserAdapter.h"
#include "handlers/RemoteAppHandler.h"
#include "handlers/RemoteLifespanHandler.h"
#include "handlers/request/RemoteRequest.h"
#include "handlers/request/RemoteResponse.h"
#include "handlers/request/RemotePostData.h"

#include "CefUtils.h"
#include "handlers/RemoteObjectFactory.h"
#include "handlers/request/RemoteAuthCallback.h"
#include "handlers/request/RemoteCallback.h"

#include "include/base/cef_callback.h"
#include "include/cef_task.h"
#include "include/wrapper/cef_closure_task.h"

using namespace apache::thrift;

namespace {
  RemoteAppHandler * g_remoteAppHandler = nullptr;
  std::thread * g_mainCefThread = nullptr;
  bool g_isInitialized = false;
}

bool isCefInitialized() { return g_isInitialized; }

ServerHandler::~ServerHandler() {
  try {
    closeAllBrowsers();
    if (myBackwardConnection != nullptr) {
      Log::debug("Close backward connection");
      myBackwardConnection->close();
      myBackwardConnection = nullptr;
    }
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
  if (myBackwardConnection == nullptr) {
    try {
      myBackwardConnection = std::make_shared<BackwardConnection>();
      myRemoteBrowsers = std::make_shared<std::vector<CefRefPtr<RemoteClientHandler>>>();
      if (g_remoteAppHandler == nullptr) {
        g_remoteAppHandler = new RemoteAppHandler(myBackwardConnection, cmdLineArgs, settings);
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

void createBrowserImpl(
    int cid, int bid, CefRefPtr<RemoteClientHandler> clienthandler,
    std::function<void()> onCreationFailed
) {
  // Should be called on UI thread
  CefWindowInfo windowInfo;
  windowInfo.SetAsWindowless(0);

  CefBrowserSettings settings;
  CefString strUrl("www.google.com");

  Log::debug( "CefBrowserHost::CreateBrowser cid=%d, bid=%d", cid, bid);
  bool result = CefBrowserHost::CreateBrowser(windowInfo, clienthandler, strUrl,
                                              settings, nullptr, nullptr);
  if (!result) {
    Log::error( "Failed to create browser with cid=%d, bid=%d", cid, bid);
    onCreationFailed();
  }
}

int32_t ServerHandler::createBrowser(int cid) {
  if (!isCefInitialized()) {
    Log::warn( "Can't create browser with cid=%d, need wait for cef initialization", cid);
    // TODO: return wrapper and schedule browser creation after initialization
    return -2;
  }

  int bid = myRemoteBrowsers->size();
  for (int c = 0, cEnd = myRemoteBrowsers->size(); c < cEnd; ++c)
    if ((*myRemoteBrowsers)[c] != nullptr) {
      bid = c;
      break;
    }

  CefRefPtr<RemoteClientHandler> clienthandler = new RemoteClientHandler(myBackwardConnection, cid, bid);
  if (bid >= 0 && bid < myRemoteBrowsers->size())
    (*myRemoteBrowsers)[bid] = clienthandler;
  else
    myRemoteBrowsers->push_back(clienthandler);

  std::function<void()> onFailed = [=](){
    (*myRemoteBrowsers)[bid] = nullptr;
    // TODO: notify client
  };
  if (CefCurrentlyOn(TID_UI)) {
    createBrowserImpl(cid, bid, clienthandler, onFailed);
  } else {
    CefPostTask(TID_UI, base::BindOnce(&createBrowserImpl, cid, bid, clienthandler, onFailed));
  }

  Log::debug("Scheduled browser creation, cid=%d, bid=%d", cid, bid);
  return bid;
}

void ServerHandler::closeBrowser(std::string& _return, const int32_t bid) {
  Log::debug("Close browser %d", bid);

  if (bid >= myRemoteBrowsers->size()) {
    Log::error("closeBrowser: bid %d > myRemoteBrowsers->size() %d", bid,
               myRemoteBrowsers->size());
    _return.assign("invalid bid");
    return;
  }
  if ((*myRemoteBrowsers)[bid] == nullptr) {
    Log::error("closeBrowser: null browser at bid %d", bid);
    _return.assign("null browser");
    return;
  }

  auto browser = getBrowser(bid);
  if (browser != nullptr)
    browser->GetHost()->CloseBrowser(true);

  (*myRemoteBrowsers)[bid] = nullptr;
}

void ServerHandler::invoke(const int32_t bid, const std::string& method, const std::string& buffer) {
  if (bid >= myRemoteBrowsers->size()) {
    Log::error("invoke: bid %d > myRemoteBrowsers->size() %d", bid,
               myRemoteBrowsers->size());
    return;
  }

  auto browser = getBrowser(bid);
  if (browser == nullptr) {
    Log::error("invoke: null browser, bid=%d", bid);
    return;
  }

  CefBrowserAdapter adapter(browser);
  adapter.setBid(bid); // for logging only
  adapter.invoke(method, buffer);
}

CefRefPtr<CefBrowser> ServerHandler::getBrowser(int bid) {
  if (bid >= myRemoteBrowsers->size()) {
    Log::error("getBrowser: bid %d > myRemoteBrowsers->size() %d", bid, myRemoteBrowsers->size());
    return nullptr;
  }

  auto ch = (*myRemoteBrowsers)[bid];
  RemoteLifespanHandler * rsh = (RemoteLifespanHandler *)(ch->GetLifeSpanHandler()).get();
  return rsh->getBrowser();
}

void ServerHandler::closeAllBrowsers() {
  std::string tmp;
  for (int bid = 0; bid < myRemoteBrowsers->size(); ++bid)
    closeBrowser(tmp, bid);
  myRemoteBrowsers->clear();
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
