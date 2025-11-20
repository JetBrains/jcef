#include "MessageRoutersManager.h"
#include "RemoteMessageRouter.h"
#include "../browser/RemoteBrowser.h"

static const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_MessageRoutersManager");

MessageRoutersManager::~MessageRoutersManager() {
  TRACE();
  // Clear all message routers (just for insurance)
  std::vector<int> toDelete;
  {
    base::AutoLock lockR(myRoutersLock);
    for (auto router : myRouters)
      toDelete.push_back(router->getId());
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

void MessageRoutersManager::add(RemoteMessageRouter* router) {
  base::AutoLock lock_scope(myRoutersLock);
  myRouters.insert(router);
}

void MessageRoutersManager::remove(RemoteMessageRouter* router) {
  base::AutoLock lock_scope(myRoutersLock);
  myRouters.erase(router);
}


