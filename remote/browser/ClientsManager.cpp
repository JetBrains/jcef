#include "ClientsManager.h"
#include "../handlers/RemoteClientHandler.h"
#include "../handlers/RemoteLifespanHandler.h"
#include "include/base/cef_callback.h"
#include "include/cef_task.h"
#include "include/wrapper/cef_closure_task.h"

ClientsManager::ClientsManager() : myRemoteClients(std::make_shared<std::vector<CefRefPtr<RemoteClientHandler>>>()) {}

CefRefPtr<RemoteClientHandler> ClientsManager::getClient(int bid) {
  return bid >= myRemoteClients->size() ? nullptr : (*myRemoteClients)[bid];
}

void ClientsManager::disposeClient(int bid) {
  if (bid >= myRemoteClients->size())
    return;
  (*myRemoteClients)[bid] = nullptr;
}

namespace {
  void createBrowserImpl(
      int cid, int bid, CefRefPtr<RemoteClientHandler> clienthandler,
      std::function<void()> onCreationFailed
  ) {
    // Should be called on UI thread
    CefWindowInfo windowInfo;
    windowInfo.SetAsWindowless(0);

    CefBrowserSettings settings;
    CefString strUrl("www.google.com");

    CefRefPtr<CefDictionaryValue> extra_info;
    auto router_configs = MessageRoutersManager::GetMessageRouterConfigs();
    if (router_configs) {
      // Send the message router config to CefHelperApp::OnBrowserCreated.
      extra_info = CefDictionaryValue::Create();
      extra_info->SetList("router_configs", router_configs);
    }

    Log::debug( "CefBrowserHost::CreateBrowser cid=%d, bid=%d", cid, bid);
    bool result = CefBrowserHost::CreateBrowser(windowInfo, clienthandler, strUrl,
                                                settings, extra_info, nullptr);
    if (!result) {
      Log::error( "Failed to create browser with cid=%d, bid=%d", cid, bid);
      onCreationFailed();
    }
  }
}

extern bool isCefInitialized();

int ClientsManager::createBrowser(int cid, std::shared_ptr<RpcExecutor> service, std::shared_ptr<MessageRoutersManager> routersManager) {
  if (!isCefInitialized()) {
    Log::warn( "Can't create browser with cid=%d, need wait for cef initialization", cid);
    // TODO: return wrapper and schedule browser creation after initialization
    return -2;
  }

  int bid = myRemoteClients->size();
  for (int c = 0, cEnd = myRemoteClients->size(); c < cEnd; ++c)
    if ((*myRemoteClients)[c] != nullptr) {
      bid = c;
      break;
    }

  CefRefPtr<RemoteClientHandler> clienthandler = new RemoteClientHandler(routersManager, service, cid, bid);
  if (bid >= 0 && bid < myRemoteClients->size())
    (*myRemoteClients)[bid] = clienthandler;
  else
    myRemoteClients->push_back(clienthandler);

  std::function<void()> onFailed = [=](){
    disposeClient(bid);
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

CefRefPtr<CefBrowser> ClientsManager::getCefBrowser(int bid) {
  CefRefPtr<RemoteClientHandler> client = getClient(bid);
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

  for (int c = 0, cEnd = myRemoteClients->size(); c < cEnd; ++c) {
    CefRefPtr<RemoteClientHandler> client = (*myRemoteClients)[c];
    if (client) {
      CefRefPtr<CefBrowser> clientBrowser = ((RemoteLifespanHandler *)(client->GetLifeSpanHandler()).get())->getBrowser();
      if (clientBrowser && clientBrowser->GetIdentifier() == browser->GetIdentifier())
        return c;
    }
  }

  return -1;
}

void ClientsManager::closeBrowser(const int32_t bid) {
  Log::debug("Close browser %d", bid);

  CefRefPtr<RemoteClientHandler> client = getClient(bid);
  if (!client) {
    Log::error("closeBrowser: can't find browser %d", bid);
    return;
  }

  auto browser = getCefBrowser(bid);
  if (browser != nullptr)
    browser->GetHost()->CloseBrowser(true);

  disposeClient(bid);
}

void ClientsManager::closeAllBrowsers() {
  for (int bid = 0; bid < myRemoteClients->size(); ++bid)
    closeBrowser(bid);
  myRemoteClients->clear();
}
