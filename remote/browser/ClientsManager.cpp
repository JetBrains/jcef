#include "ClientsManager.h"
#include "../CefUtils.h"
#include "../handlers/RemoteClientHandler.h"
#include "../handlers/RemoteLifespanHandler.h"
#include "include/base/cef_callback.h"
#include "include/cef_task.h"
#include "include/wrapper/cef_closure_task.h"

ClientsManager::ClientsManager() : myRemoteClients() {}

namespace {
  void createBrowserImpl(
      int cid, int bid, CefRefPtr<RemoteClientHandler> clienthandler,
      const std::string& url,
      std::function<void()> onCreationFailed
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

    Log::trace( "CefBrowserHost::CreateBrowser cid=%d, bid=%d", cid, bid);
    bool result = CefBrowserHost::CreateBrowser(windowInfo, clienthandler, url,
                                                settings, extra_info, nullptr);
    if (!result) {
      Log::error( "Failed to create browser with cid=%d, bid=%d", cid, bid);
      onCreationFailed();
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
  int bid;
  CefRefPtr<RemoteClientHandler> clienthandler;
  {
    Lock lock(myMutex);
    static int sBid = 0;
    bid = sBid++;
    clienthandler = new RemoteClientHandler(routersManager, service, serviceIO, cid, bid);
    myRemoteClients[bid] = clienthandler;
  }

  std::function<void()> onFailed = [=](){
    Lock lock(myMutex);
    myRemoteClients.erase(bid);
  };
  if (CefCurrentlyOn(TID_UI)) {
    createBrowserImpl(cid, bid, clienthandler, url, onFailed);
  } else {
    CefPostTask(TID_UI, base::BindOnce(&createBrowserImpl, cid, bid, clienthandler, url, onFailed));
  }

  Log::debug("Started native CefBrowser creation, cid=%d, bid=%d", cid, bid);
  return bid;
}

CefRefPtr<CefBrowser> ClientsManager::getCefBrowser(int bid) {
  Lock lock(myMutex);
  CefRefPtr<RemoteClientHandler> client = myRemoteClients[bid];
  if (!client) {
    Log::error("getCefBrowser: can't find client by bid %d", bid);
    return nullptr;
  }

  RemoteLifespanHandler * rsh = (RemoteLifespanHandler *)(client->GetLifeSpanHandler()).get();
  return rsh->getBrowser();
}

int ClientsManager::findRemoteBrowser(CefRefPtr<CefBrowser> browser) {
  if (!browser)
    return -1;

  Lock lock(myMutex);
  for (auto const& rc : myRemoteClients) {
    CefRefPtr<RemoteClientHandler> client = rc.second;
    if (client) {
      CefRefPtr<CefBrowser> clientBrowser = ((RemoteLifespanHandler *)(client->GetLifeSpanHandler()).get())->getBrowser();
      if (clientBrowser && clientBrowser->GetIdentifier() == browser->GetIdentifier())
        return rc.first;
    }
  }

  return -1;
}

void ClientsManager::closeBrowser(const int32_t bid) {
  Lock lock(myMutex);
  CefRefPtr<RemoteClientHandler> client = myRemoteClients[bid];
  if (!client) {
    // already closed
    return;
  }

  RemoteLifespanHandler * rsh = (RemoteLifespanHandler *)(client->GetLifeSpanHandler()).get();
  auto browser = rsh->getBrowser();
  if (browser != nullptr)
    browser->GetHost()->CloseBrowser(true);

  myRemoteClients.erase(bid);
}

void ClientsManager::closeAllBrowsers() {
  Lock lock(myMutex);
  std::vector<int> toRemove;
  for (auto const& rc : myRemoteClients)
    toRemove.push_back(rc.first);

  for (auto const& bid : toRemove)
    closeBrowser(bid);

  myRemoteClients.clear();
}
