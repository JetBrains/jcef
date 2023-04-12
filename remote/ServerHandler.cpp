#include "ServerHandler.h"

#include <thrift/transport/TSocket.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TTransportUtils.h>

#include "RemoteRenderHandler.h"
#include "RemoteLifespanHandler.h"

BackwardConnection::BackwardConnection() {
  myTransport = std::make_shared<TBufferedTransport>(std::make_shared<TSocket>("localhost", 9091));
  myClientHandlers = std::make_shared<ClientHandlersClient>(std::make_shared<TBinaryProtocol>(myTransport));

  myTransport->open();
  const int32_t backwardCid = myClientHandlers->connect();
  Log::debug("backward connection to client established, backwardCid=%d", backwardCid);
}

void BackwardConnection::close() {
  if (myClientHandlers != nullptr) {
    myClientHandlers = nullptr;

    myTransport->close();
    myTransport = nullptr;
  }
}

ServerHandler::~ServerHandler() {
  try {
    closeAllBrowsers();
    if (myBackwardConnection != nullptr) {
      Log::debug("close backward connection");
      myBackwardConnection->close();
      myBackwardConnection = nullptr;
    }
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
    } catch (TException& tx) {
      Log::error(tx.what());
      return -1;
    }
  }

  return cid++;
}

int32_t ServerHandler::createBrowser() {
  int bid = myRemoteBrowsers.size();
  for (int c = 0, cEnd = myRemoteBrowsers.size(); c < cEnd; ++c)
    if (myRemoteBrowsers[c] != nullptr) {
      bid = c;
      break;
    }

  CefRefPtr<RemoteClientHandler> clienthandler = new RemoteClientHandler(new RemoteRenderHandler(myBackwardConnection, bid));

  CefWindowInfo windowInfo;
  windowInfo.SetAsWindowless(0);

  CefBrowserSettings settings;
  CefString strUrl("www.google.com");

  bool result = CefBrowserHost::CreateBrowser(windowInfo, clienthandler, strUrl,
                                              settings, nullptr, nullptr);
  if (!result) {
    Log::error( "failed to create browser with bid=%d", bid);
    return -1;
  }
  Log::debug("browser successfully created, bid=%d", bid);

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

  if (method.compare("wasresized") == 0) {
    browser->GetHost()->WasResized();
  } else if (method.compare("sendmouseevent") == 0) {
    const int len = buffer.size();
    if (len < 4) {
      Log::error("sendmouseevent, len %d < 4", len);
      return;
    }

    const int32_t * p = (const int32_t *)buffer.c_str();
    int event_type = *(p++);
    int modifiers = *(p++);

    CefMouseEvent cef_event;
    cef_event.x = *(p++);
    cef_event.y = *(p++);

    // TODO: read modifiers and other params
    CefBrowserHost::MouseButtonType cef_mbt = MBT_LEFT;
    browser->GetHost()->SendMouseClickEvent(cef_event, cef_mbt, event_type == 0, 1);
  } else if (method.compare("sendkeyevent") == 0) {
    CefKeyEvent cef_event;
    // TODO: read modifiers and other params
    browser->GetHost()->SendKeyEvent(cef_event);
  }
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
