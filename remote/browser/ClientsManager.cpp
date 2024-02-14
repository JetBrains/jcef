#include "ClientsManager.h"
#include "../ServerHandler.h"
#include "../handlers/RemoteLifespanHandler.h"
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
                                                settings, extra_info, nullptr);
    if (!result) {
      Log::error( "Failed to create browser with cid=%d, bid=%d", cid, bid);
      onCreationFailed(bid);
    }
  }
}

int ClientsManager::createBrowser(
    int cid,
    std::shared_ptr<RpcExecutor> service,
    std::shared_ptr<RpcExecutor> serviceIO,
    std::shared_ptr<MessageRoutersManager> routersManager,
    const std::string& url
) {
  CefRefPtr<RemoteClientHandler> clienthandler;
  // TODO: to prevent possible leaks implement:
  // 1. wait some time for callback
  // 2. force clear all clients
  std::shared_ptr<ClientsStorage> storage = myRemoteClients;
  std::function<void(int)> remove = [=](int bid){
    storage->erase(bid);
  };

  int bid;
  {
    Lock lock(myRemoteClients->myMutex);
    static int sBid = 0;
    bid = sBid++;
    clienthandler = new RemoteClientHandler(routersManager, service, serviceIO, cid, bid, remove);
    myRemoteClients->set(bid, clienthandler);
  }

  if (CefCurrentlyOn(TID_UI)) {
    createBrowserImpl(cid, bid, clienthandler, url, remove);
  } else {
    CefPostTask(TID_UI, base::BindOnce(&createBrowserImpl, cid, bid, clienthandler, url, remove));
  }

  return bid;
}

CefRefPtr<CefBrowser> ClientsManager::getCefBrowser(int bid) {
  CefRefPtr<RemoteClientHandler> client = myRemoteClients->get(bid);
  if (!client) {
    Log::error("getCefBrowser: can't find client by bid %d", bid);
    return nullptr;
  }

  return client->getCefBrowser();
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

void ClientsManager::closeAllBrowsers() {
  myRemoteClients->closeAll();
}

CefRefPtr<RemoteClientHandler> ClientsManager::ClientsStorage::get(int bid) {
  Lock lock(myMutex);
  return myBid2Client[bid];
}

void ClientsManager::ClientsStorage::checkShuttingDown() {
  if (ServerHandler::isShuttingDown()) {
    Lock lock(myMutex);
    if (myBid2Client.empty())
      ServerHandler::setStateShutdown();
    else {
      std::stringstream ss("remain browsers: ");
      for (auto const& rc : myBid2Client) {
        CefRefPtr<RemoteClientHandler> client = myBid2Client[rc.first];
        if (client) ss << client->getBid() << ", ";
      }
      ServerHandler::setStateDesc("shutting down (" + ss.str() + ")");
      Log::debug("Shutting down, wait closing %s", ss.str().c_str());
    }
  }
}

void ClientsManager::ClientsStorage::closeAll() {
  Lock lock(myMutex);
  checkShuttingDown();
  for (auto const& rc : myBid2Client) {
    CefRefPtr<RemoteClientHandler> client = myBid2Client[rc.first];
    if (client)
      client->closeBrowser();
  }
}

void ClientsManager::ClientsStorage::set(int bid, CefRefPtr<RemoteClientHandler> val) {
  // Called under mutex lock
  myBid2Client[bid] = val;
}

void ClientsManager::ClientsStorage::erase(int bid) {
  Lock lock(myMutex);
  myBid2Client.erase(bid);
  checkShuttingDown();
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
