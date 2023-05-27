#ifndef JCEF_MESSAGEROUTERSMANAGER_H
#define JCEF_MESSAGEROUTERSMANAGER_H
#include <set>
#include "include/cef_browser.h"
#include "include/wrapper/cef_message_router.h"

// comparator to check if configuration values are the same
struct cmpCfg {
  bool operator()(const CefMessageRouterConfig& lValue,
                  const CefMessageRouterConfig& rValue) const {
    std::less<std::string> comp;
    return comp(lValue.js_query_function.ToString(),
                rValue.js_query_function.ToString());
  }
};
class RemoteMessageRouter;
class RpcExecutor;

// Manages lifetime of stored routers
class MessageRoutersManager {
 public:
  RemoteMessageRouter * CreateRemoteMessageRouter(std::shared_ptr<RpcExecutor> service, const std::string& query, const std::string& cancel);
  void DisposeRemoteMessageRouter(int objId);
  virtual ~MessageRoutersManager();

  // Next 4 methods should be called from corresponding handlers of CefClient
  void OnBeforeClose(CefRefPtr<CefBrowser> browser);
  void OnBeforeBrowse(CefRefPtr<CefBrowser> browser, CefRefPtr<CefFrame> frame);
  void OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser);
  bool OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                                CefRefPtr<CefFrame> frame,
                                CefProcessId source_process,
                                CefRefPtr<CefProcessMessage> message);

  static CefRefPtr<CefListValue> GetMessageRouterConfigs();
  static void ClearAllConfigs();

 private:
  std::set<RemoteMessageRouter*> myRouters;
  base::Lock myRoutersLock;

  static std::set<CefMessageRouterConfig, cmpCfg> router_cfg_;
  static base::Lock router_cfg_lock_;
};

#endif  // JCEF_MESSAGEROUTERSMANAGER_H
