#include "ClientsManager.h"
#include "../ServerApplication.h"
#include "../handlers/RemoteClientHandler.h"
#include "../router/MessageRoutersManager.h"
#include "include/base/cef_callback.h"
#include "include/cef_app.h"
#include "include/cef_task.h"
#include "include/wrapper/cef_closure_task.h"

ClientsManager::ClientsManager() : myRemoteClients(std::make_shared<ClientsStorage>()) {}

namespace {
  // Should be called on UI thread
  void createCefBrowserImpl(
      int cid, int bid, CefRefPtr<RemoteClientHandler> clienthandler,
      std::string url,
      std::function<void(int)> onCreationFailed
  ) {
    CefBrowserSettings settings; // TODO: get real CefBrowserSettings from java
    CefWindowInfo windowInfo;
    windowInfo.SetAsWindowless(0);
    // JCEF requires Alloy runtime style for "normal" browsers in order for them
    // to be integratable into Java UI.
    windowInfo.runtime_style = CEF_RUNTIME_STYLE_ALLOY;

    CefRefPtr<CefDictionaryValue> extra_info;
    auto router_configs = MessageRoutersManager::GetMessageRouterConfigs();
    if (router_configs) {
      // Send the message router config to CefHelperApp::OnBrowserCreated.
      extra_info = CefDictionaryValue::Create();
      extra_info->SetList("router_configs", router_configs);
    }

    //Log::trace( "CefBrowserHost::CreateBrowser cid=%d, bid=%d", cid, bid);
    bool result = CefBrowserHost::CreateBrowser(windowInfo, clienthandler, url,
                                                settings, extra_info, clienthandler->getRequestContext());
    if (!result) {
      Log::error( "Failed to create browser with cid=%d, bid=%d", cid, bid);
      onCreationFailed(bid);
    }
  }

  // Should be called on UI thread
  void openDevToolsPopupImpl(
      int cid, int bid, CefRefPtr<RemoteClientHandler> clienthandler,
      CefRefPtr<CefBrowser> parentBrowser,
      CefPoint inspectAt
  ) {
    if (!parentBrowser)
      return;

    Log::trace( "ShowDevTools: cid=%d, bid=%d, pt=(%d,%d)", cid, bid, inspectAt.x, inspectAt.y);
    CefWindowInfo windowInfo;
    CefBrowserSettings settings; // TODO: get real CefBrowserSettings from java
    parentBrowser->GetHost()->ShowDevTools(windowInfo, nullptr, settings, inspectAt);
  }
}

int ClientsManager::createBrowser(
    int cid,
    std::shared_ptr<ServerHandlerContext> ctx,
    int handlersMask, const thrift_codegen::RObject& requestContextHandler
) {
  CefRefPtr<RemoteClientHandler> clienthandler;
  int bid;
  {
    Lock lock(myRemoteClients->myMutex);
    static int sBid = 0;
    bid = sBid++;
    clienthandler = new RemoteClientHandler(ctx, cid, bid, handlersMask, requestContextHandler);
    myRemoteClients->set(bid, clienthandler);
  }

  return bid;
}

void ClientsManager::startNativeBrowserCreation(int bid, const std::string & url) {
  CefRefPtr<RemoteClientHandler> clienthandler = myRemoteClients->get(bid);
  if (!clienthandler)
    return;

  std::shared_ptr<ClientsStorage> storage = myRemoteClients;
  std::function<void(int)> remove = [=](int bid){
    storage->erase(bid);
  };
  if (CefCurrentlyOn(TID_UI)) {
    createCefBrowserImpl(clienthandler->getCid(), bid, clienthandler,
                         url, remove);
  } else {
    CefPostTask(TID_UI, base::BindOnce(&createCefBrowserImpl, clienthandler->getCid(), bid, clienthandler, url, remove));
  }
}

void ClientsManager::openDevTools(int bid, int x, int y) {
  CefRefPtr<RemoteClientHandler> parentHandler = myRemoteClients->get(bid);
  if (!parentHandler)
    return;

  CefRefPtr<CefBrowser> parentBrowser = parentHandler->getCefBrowser();
  if (!parentBrowser) {
    Log::error("Can't open dev-tools for bid=%d, because native CefBrowser wasn't created yet.", bid);
    return;
  }

  if (CefCurrentlyOn(TID_UI)) {
    openDevToolsPopupImpl(parentHandler->getCid(), bid, parentHandler, parentBrowser, CefPoint(x, y));
  } else {
    CefPostTask(TID_UI, base::BindOnce(&openDevToolsPopupImpl, parentHandler->getCid(), bid, parentHandler, parentBrowser, CefPoint(x, y)));
  }
}

CefRefPtr<CefBrowser> ClientsManager::getCefBrowser(int bid) {
  CefRefPtr<RemoteClientHandler> client = myRemoteClients->get(bid);
  if (!client) {
    Log::error("getCefBrowser: can't find client by bid %d", bid);
    return nullptr;
  }

  return client->getCefBrowser();
}

CefRefPtr<RemoteClientHandler> ClientsManager::getClient(int bid) {
  CefRefPtr<RemoteClientHandler> client = myRemoteClients->get(bid);
  if (!client) {
    Log::error("getClient: can't find client by bid %d", bid);
    return nullptr;
  }

  return client;
}

int ClientsManager::findRemoteBrowser(CefRefPtr<CefBrowser> browser) {
  return myRemoteClients->findRemoteBrowser(browser);
}

void ClientsManager::closeBrowser(const int32_t bid) {
  CefRefPtr<RemoteClientHandler> client = myRemoteClients->get(bid);
  if (!client) {
    Log::error("Remote browser is already closed, bid=%d", bid);
    return;
  }

  client->closeBrowser();
}

bool ClientsManager::closeAllBrowsers() {
  return myRemoteClients->closeAll();
}

std::vector<int> ClientsManager::enumAllBrowsers() {
  return myRemoteClients->enumClients();
}

void ClientsManager::erase(int bid) {
  myRemoteClients->erase(bid);
}

CefRefPtr<RemoteClientHandler> ClientsManager::ClientsStorage::get(int bid) {
  Lock lock(myMutex);
  return myBid2Client[bid];
}

std::vector<int> ClientsManager::ClientsStorage::enumClients() {
  Lock lock(myMutex);
  std::vector<int> result;
  for (auto const& rc : myBid2Client) {
    CefRefPtr<RemoteClientHandler> client = rc.second;
    if (client)
      result.push_back(client->getBid());
  }
  return result;
}

bool ClientsManager::ClientsStorage::closeAll() {
  bool isEmpty = true;
  Lock lock(myMutex);
  for (auto const& rc : myBid2Client) {
    CefRefPtr<RemoteClientHandler> client = rc.second;
    if (client) {
      client->closeBrowser();
      isEmpty = false;
    }
  }
  return isEmpty;
}

void ClientsManager::ClientsStorage::set(int bid, CefRefPtr<RemoteClientHandler> val) {
  // Called under mutex lock
  myBid2Client[bid] = val;
}

void ClientsManager::ClientsStorage::erase(int bid) {
  {
    Lock lock(myMutex);
    myBid2Client.erase(bid);
  }
  ServerApplication::instance().onRemoteClientHandlerDestroyed();
}

int ClientsManager::ClientsStorage::findRemoteBrowser(CefRefPtr<CefBrowser> browser) {
  if (!browser)
    return -1;

  Lock lock(myMutex);
  for (auto const& rc : myBid2Client) {
    CefRefPtr<RemoteClientHandler> client = rc.second;
    if (client) {
      CefRefPtr<CefBrowser> clientBrowser = client->getCefBrowser();
      if (clientBrowser && clientBrowser->GetIdentifier() == browser->GetIdentifier())
        return rc.first;
    }
  }

  return -1;
}
