#include "ClientsManager.h"
#include "../ServerState.h"
#include "../handlers/RemoteClientHandler.h"
#include "../router/MessageRoutersManager.h"
#include "include/base/cef_callback.h"
#include "include/cef_task.h"
#include "include/wrapper/cef_closure_task.h"
#include "include/cef_app.h"

ClientsManager::ClientsManager() : myRemoteClients(std::make_shared<ClientsStorage>()) {}

namespace {
  void createBrowserImpl(
      int cid, int bid, CefRefPtr<RemoteClientHandler> clienthandler,
      const std::string& url,
      std::function<void(int)> onCreationFailed
  ) {
    // Should be called on UI thread
    CefWindowInfo windowInfo;
    windowInfo.SetAsWindowless(0);

    CefBrowserSettings settings;

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
    createBrowserImpl(clienthandler->getCid(), bid, clienthandler, url, remove);
  } else {
    CefPostTask(TID_UI, base::BindOnce(&createBrowserImpl, clienthandler->getCid(), bid, clienthandler, url, remove));
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

std::string ClientsManager::closeAllBrowsers() {
  return myRemoteClients->closeAll();
}

void ClientsManager::erase(int bid) {
  myRemoteClients->erase(bid);
}

CefRefPtr<RemoteClientHandler> ClientsManager::ClientsStorage::get(int bid) {
  Lock lock(myMutex);
  return myBid2Client[bid];
}

std::string ClientsManager::ClientsStorage::enumClients() {
  std::stringstream ss;
  for (auto const& rc : myBid2Client) {
    CefRefPtr<RemoteClientHandler> client = rc.second;
    if (client)
      ss << client->getBid() << ", ";
  }
  return ss.str();
}

std::string ClientsManager::ClientsStorage::closeAll() {
  Lock lock(myMutex);
  for (auto const& rc : myBid2Client) {
    CefRefPtr<RemoteClientHandler> client = rc.second;
    if (client)
      client->closeBrowser();
  }
  return enumClients();
}

void ClientsManager::ClientsStorage::set(int bid, CefRefPtr<RemoteClientHandler> val) {
  // Called under mutex lock
  myBid2Client[bid] = val;
}

void ClientsManager::ClientsStorage::erase(int bid) {
  Lock lock(myMutex);
  myBid2Client.erase(bid);
  ServerState::instance().onClientDestroyed(enumClients());
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
