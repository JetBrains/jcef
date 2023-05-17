#include "MessageRoutersManager.h"
#include "RemoteMessageRouter.h"

// remove to enable tracing
//#define TRACE()

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
  std::set<CefRefPtr<CefMessageRouterBrowserSide>> message_routers;
  {
    base::AutoLock lock_scope(myRoutersLock);
    message_routers = myRouters;
  }

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
  base::AutoLock lock_scope(myRoutersLock);
  for (auto& router : myRouters) {
    router->OnBeforeClose(browser);
  }
}

void MessageRoutersManager::OnBeforeBrowse(CefRefPtr<CefBrowser> browser, CefRefPtr<CefFrame> frame) {
  TRACE();
  // NOTE: invoked on UI thread
  base::AutoLock lock_scope(myRoutersLock);
  for (auto& router : myRouters) {
    router->OnBeforeBrowse(browser, frame);
  }
}

void MessageRoutersManager::OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser) {
  TRACE();
  // NOTE: invoked on UI thread
  base::AutoLock lock_scope(myRoutersLock);
  for (auto& router : myRouters) {
    router->OnRenderProcessTerminated(browser);
  }
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

void MessageRoutersManager::ClearAllConfigs() {
  TRACE();
  base::AutoLock lock_scope(router_cfg_lock_);
  router_cfg_.clear();
}

// TODO: add leak protection: dispose all created routers is ~MessageRoutersManager
RemoteMessageRouter * MessageRoutersManager::CreateRemoteMessageRouter(std::shared_ptr<RpcExecutor> service, const std::string& query, const std::string& cancel) {
  TRACE();
  CefMessageRouterConfig config;
  config.js_query_function = query;
  config.js_cancel_function = cancel;
  CefRefPtr<CefMessageRouterBrowserSide> msgRouter = CefMessageRouterBrowserSide::Create(config);

  {
    base::AutoLock lock_scope(myRoutersLock);
    myRouters.insert(msgRouter);
  }

  {
    base::AutoLock lock_scope(router_cfg_lock_);
    router_cfg_.insert(config);
  }

  return RemoteMessageRouter::create(service, msgRouter, config);
}

void MessageRoutersManager::DisposeRemoteMessageRouter(int objId) {
  TRACE();
  RemoteMessageRouter * rmr = RemoteMessageRouter::get(objId);
  if (rmr == nullptr) return;

  {
    base::AutoLock lock_scope(myRoutersLock);
    myRouters.erase(CefRefPtr<CefMessageRouterBrowserSide>(&(rmr->getDelegate())));
  }

  {
    base::AutoLock lock_scope(router_cfg_lock_);
    router_cfg_.erase(rmr->getConfig());
  }
  RemoteMessageRouter::dispose(objId);
}