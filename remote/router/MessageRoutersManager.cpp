#include "MessageRoutersManager.h"
#include "RemoteMessageRouter.h"

static const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_MessageRoutersManager");

MessageRoutersManager::~MessageRoutersManager() {
  TRACE();
  std::vector<int> toDelete;
  {
    base::AutoLock lockR(myRoutersLock);
    base::AutoLock lockC(router_cfg_lock_);
    for (auto router : myRouters) {
      router_cfg_.erase(router->getConfig());
      toDelete.push_back(router->getId());
    }
    myRouters.clear();
  }

  for (int id: toDelete)
    RemoteMessageRouter::dispose(id);
}

std::set<CefRefPtr<CefMessageRouterBrowserSide>> MessageRoutersManager::getMessageRouters() {
  TRACE();
  std::set<CefRefPtr<CefMessageRouterBrowserSide>> message_routers;
  base::AutoLock lock_scope(myRoutersLock);
  for (auto r: myRouters)
    message_routers.insert(CefRefPtr<CefMessageRouterBrowserSide>(&r->getDelegate()));
  return message_routers;
}

bool MessageRoutersManager::OnProcessMessageReceived(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefProcessId source_process,
    CefRefPtr<CefProcessMessage> message) {
  TRACE();
  bool handled = false;
  
  // Iterate on a copy of |myRouters| to avoid re-entrancy of
  // |myRoutersLock| if the client CefMessageRouterHandler impl
  // calls CefClientHandler.addMessageRouter/removeMessageRouter.
  std::set<CefRefPtr<CefMessageRouterBrowserSide>> message_routers = getMessageRouters();

  for (auto& router : message_routers) {
    handled = router->OnProcessMessageReceived(browser, frame, source_process, message);
    if (handled)
      break;
  }
  return handled;
}

void MessageRoutersManager::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  TRACE();
  // NOTE: invoked on UI thread
  std::set<CefRefPtr<CefMessageRouterBrowserSide>> message_routers = getMessageRouters();
  for (auto& router : message_routers)
    router->OnBeforeClose(browser);
}

void MessageRoutersManager::OnBeforeBrowse(CefRefPtr<CefBrowser> browser, CefRefPtr<CefFrame> frame) {
  TRACE();
  // NOTE: invoked on UI thread
  std::set<CefRefPtr<CefMessageRouterBrowserSide>> message_routers = getMessageRouters();
  for (auto& router : message_routers)
    router->OnBeforeBrowse(browser, frame);
}

void MessageRoutersManager::OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser) {
  TRACE();
  // NOTE: invoked on UI thread
  std::set<CefRefPtr<CefMessageRouterBrowserSide>> message_routers = getMessageRouters();
  for (auto& router : message_routers)
    router->OnRenderProcessTerminated(browser);
}

// instantiate static values
std::set<CefMessageRouterConfig, cmpCfg> MessageRoutersManager::router_cfg_;
base::Lock MessageRoutersManager::router_cfg_lock_;

CefRefPtr<CefListValue> MessageRoutersManager::GetMessageRouterConfigs() {
  TRACE();
  int idx = 0;
  static std::set<CefMessageRouterConfig, cmpCfg>::iterator iter;

  base::AutoLock lock_scope(router_cfg_lock_);
  if (router_cfg_.empty())
    return nullptr;

  // Configuration passed to CefHelperApp::OnBrowserCreated.
  auto router_configs = CefListValue::Create();
  for (iter = router_cfg_.begin(); iter != router_cfg_.end(); ++iter) {
    CefRefPtr<CefDictionaryValue> dict = CefDictionaryValue::Create();
    dict->SetString("js_query_function", iter->js_query_function);
    dict->SetString("js_cancel_function", iter->js_cancel_function);
    router_configs->SetDictionary(idx, dict);
    idx++;
  }

  return router_configs;
}

RemoteMessageRouter * MessageRoutersManager::CreateRemoteMessageRouter(std::shared_ptr<ServerHandlerContext> ctx, const std::string& query, const std::string& cancel) {
  TRACE();
  CefMessageRouterConfig config;
  config.js_query_function = query;
  config.js_cancel_function = cancel;
  CefRefPtr<CefMessageRouterBrowserSide> msgRouter = CefMessageRouterBrowserSide::Create(config);
  RemoteMessageRouter * result = RemoteMessageRouter::create(ctx, msgRouter, config);
  {
    base::AutoLock lock_scope(myRoutersLock);
    myRouters.insert(result);
  }

  {
    base::AutoLock lock_scope(router_cfg_lock_);
    router_cfg_.insert(config);
  }

  return result;
}

void MessageRoutersManager::DisposeRemoteMessageRouter(int objId) {
  TRACE();
  RemoteMessageRouter * rmr = RemoteMessageRouter::get(objId);
  if (rmr == nullptr) return;

  {
    base::AutoLock lock_scope(myRoutersLock);
    myRouters.erase(rmr);
  }

  {
    base::AutoLock lock_scope(router_cfg_lock_);
    router_cfg_.erase(rmr->getConfig());
  }
  RemoteMessageRouter::dispose(objId);
}

