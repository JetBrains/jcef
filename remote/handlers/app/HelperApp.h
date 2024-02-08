#ifndef JCEF_HELPERAPP_H
#define JCEF_HELPERAPP_H

#include "include/cef_app.h"
#include "include/wrapper/cef_message_router.h"

class HelperApp : public CefApp, public CefRenderProcessHandler {
#ifdef LOG_VIA_THRIFT
  // just for convenience (debug logging)
  std::shared_ptr<ServerClient> myService = nullptr;
  std::shared_ptr<apache::thrift::transport::TTransport> myTransport;
  std::recursive_mutex myMutex;

  void log(std::string msg) {
    Lock lock(myMutex);
    myService->log(msg);
  }
#endif // LOG_VIA_THRIFT
 public:
  HelperApp();

  void OnRegisterCustomSchemes(CefRawPtr<CefSchemeRegistrar> registrar) override;
  virtual CefRefPtr<CefRenderProcessHandler> GetRenderProcessHandler() override {
    return this;
  }

  void OnBrowserCreated(CefRefPtr<CefBrowser> browser, CefRefPtr<CefDictionaryValue> extra_info) override;

  void OnContextCreated(CefRefPtr<CefBrowser> browser,
                        CefRefPtr<CefFrame> frame,
                        CefRefPtr<CefV8Context> context) override;

  void OnContextReleased(CefRefPtr<CefBrowser> browser,
                         CefRefPtr<CefFrame> frame,
                         CefRefPtr<CefV8Context> context) override;

  bool OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                                CefRefPtr<CefFrame> frame,
                                CefProcessId source_process,
                                CefRefPtr<CefProcessMessage> message) override;

 private:
  // comparator to check if configuration values are the same
  struct cmpCfg {
    bool operator()(const CefMessageRouterConfig& lValue,
                    const CefMessageRouterConfig& rValue) const {
      std::less<std::string> comp;
      return comp(lValue.js_query_function.ToString(),
                  rValue.js_query_function.ToString());
    }
  };

  std::map<CefMessageRouterConfig,
           CefRefPtr<CefMessageRouterRendererSide>,
           cmpCfg>
      message_router_;

  IMPLEMENT_REFCOUNTING(HelperApp);
};


#endif  // JCEF_HELPERAPP_H
