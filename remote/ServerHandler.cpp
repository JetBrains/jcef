#include "ServerHandler.h"

#include <thread>

#include "RemoteLifespanHandler.h"
#include "RemoteAppHandler.h"
#include "CefBrowserAdapter.h"

#include "CefUtils.h"

using namespace apache::thrift;

namespace {
  RemoteAppHandler * g_remoteAppHandler = nullptr;
  std::thread * g_mainCefThread = nullptr;
}

ServerHandler::~ServerHandler() {
  try {
    closeAllBrowsers();
    if (myBackwardConnection != nullptr) {
      Log::debug("close backward connection");
      myBackwardConnection->close();
      myBackwardConnection = nullptr;
    }
    // TODO: probably we should shutdown cef (so AppHandler will update on next intialization)
  } catch (TException e) {
    Log::error("thrift exception in ~ServerHandler: %s", e.what());
  }
}

int32_t ServerHandler::connect() {
  static int cid = 0;
  Log::debug("connected new client with cid=%d", cid++);

  // Connect to client's side (for cef-callbacks execution on java side)
  if (myBackwardConnection == nullptr) {
    try {
      myBackwardConnection = std::make_shared<BackwardConnection>();
      if (g_remoteAppHandler == nullptr) {
        Log::debug("Start cef initialization");
        g_remoteAppHandler = new RemoteAppHandler(myBackwardConnection);
        g_mainCefThread = new std::thread([=]() {
          doCefInitializeAndRun(g_remoteAppHandler);
        });
      } else {
        Log::error("Cef has been initialized and CefApp handler from new client connection will be ignored");
      }
    } catch (TException& tx) {
      Log::error(tx.what());
      return -1;
    }
  }

  return cid++;
}

int32_t ServerHandler::createBrowser(int cid) {
  if (!isCefInitialized()) {
    Log::error( "Can't create browser with cid=%d, need wait for cef initialization", cid);
    // TODO: return wrapper and schedule browser creation after initialization
    return -2;
  }

  int bid = myRemoteBrowsers.size();
  for (int c = 0, cEnd = myRemoteBrowsers.size(); c < cEnd; ++c)
    if (myRemoteBrowsers[c] != nullptr) {
      bid = c;
      break;
    }

  CefRefPtr<RemoteClientHandler> clienthandler = new RemoteClientHandler(myBackwardConnection, cid, bid);

  CefWindowInfo windowInfo;
  windowInfo.SetAsWindowless(0);

  CefBrowserSettings settings;
  CefString strUrl("www.google.com");

  bool result = CefBrowserHost::CreateBrowser(windowInfo, clienthandler, strUrl,
                                              settings, nullptr, nullptr);
  if (!result) {
    Log::error( "failed to create browser with cid=%d, bid=%d", cid, bid);
    return -1;
  }
  Log::debug("browser successfully created, cid=%d, bid=%d", cid, bid);

  if (bid >= 0 && bid < myRemoteBrowsers.size())
    myRemoteBrowsers[bid] = clienthandler;
  else
    myRemoteBrowsers.push_back(clienthandler);

  return bid;
}

void ServerHandler::closeBrowser(std::string& _return, const int32_t bid) {
  Log::debug("close browser %d", bid);

  if (bid >= myRemoteBrowsers.size()) {
    Log::debug("closeBrowser: bid %d > myRemoteBrowsers.size() %d", bid,
               myRemoteBrowsers.size());
    _return.assign("invalid bid");
    return;
  }
  if (myRemoteBrowsers[bid] == nullptr) {
    Log::debug("closeBrowser: null browser at bid %d", bid);
    _return.assign("null browser");
    return;
  }

  auto browser = getBrowser(bid);
  if (browser != nullptr)
    browser->GetHost()->CloseBrowser(true);

  myRemoteBrowsers[bid] = nullptr;
}

void ServerHandler::invoke(const int32_t bid, const std::string& method, const std::string& buffer) {
  if (bid >= myRemoteBrowsers.size()) {
    Log::debug("invoke: bid %d > myRemoteBrowsers.size() %d", bid,
               myRemoteBrowsers.size());
    return;
  }

  auto browser = getBrowser(bid);
  if (browser == nullptr) {
    Log::debug("invoke: null browser, bid=%d", bid);
    return;
  }

  CefBrowserAdapter adapter(browser);
  adapter.setBid(bid); // for logging only
  adapter.invoke(method, buffer);
}

CefRefPtr<CefBrowser> ServerHandler::getBrowser(int bid) {
  if (bid >= myRemoteBrowsers.size()) {
    Log::error("getBrowser: bid %d > myRemoteBrowsers.size() %d", bid, myRemoteBrowsers.size());
    return nullptr;
  }

  auto ch = myRemoteBrowsers[bid];
  RemoteLifespanHandler * rsh = (RemoteLifespanHandler *)(ch->GetLifeSpanHandler()).get();
  return rsh->getBrowser();
}

void ServerHandler::closeAllBrowsers() {
  std::string tmp;
  for (int bid = 0; bid < myRemoteBrowsers.size(); ++bid)
    closeBrowser(tmp, bid);
  myRemoteBrowsers.clear();
}
