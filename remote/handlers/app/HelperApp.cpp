#include "HelperApp.h"
#include <fstream>
#include "../../Utils.h"

//#define LOG_VIA_THRIFT

HelperApp::HelperApp()
{
#ifdef LOG_VIA_THRIFT
  myTransport = std::make_shared<TBufferedTransport>(std::make_shared<TSocket>("localhost", 9090));
  myService = std::make_shared<ServerClient>(std::make_shared<TBinaryProtocol>(myTransport));
  myTransport->open();
  log("Helper process connected.");
#endif // LOG_VIA_THRIFT
}

void HelperApp::OnRegisterCustomSchemes(CefRawPtr<CefSchemeRegistrar> registrar) {
  std::fstream fStream;
  std::string fName = utils::GetTempFile("scheme", true);
  char schemeName[512] = "";
  int options;

  fStream.open(fName.c_str(), std::fstream::in);
  while (fStream.is_open() && !fStream.eof()) {
    fStream.getline(schemeName, 512, ',');
    if (strlen(schemeName) == 0)
      break;

    fStream >> options;
    registrar->AddCustomScheme(schemeName, options);
  }
  fStream.close();
}

void HelperApp::OnBrowserCreated(CefRefPtr<CefBrowser> browser, CefRefPtr<CefDictionaryValue> extra_info) {
#ifdef LOG_VIA_THRIFT
  log("OnBrowserCreated");
#endif // LOG_VIA_THRIFT
  if (!extra_info) {
    return;
  }
  auto router_configs = extra_info->GetList("router_configs");
  if (router_configs) {
    // Configuration from BrowserProcessHandler::GetMessageRouterConfigs.
    for (size_t idx = 0; idx < router_configs->GetSize(); idx++) {
      CefRefPtr<CefDictionaryValue> dict =
          router_configs->GetDictionary((int)idx);
      // Create the renderer-side router for query handling.
      CefMessageRouterConfig config;
      config.js_query_function = dict->GetString("js_query_function");
      config.js_cancel_function = dict->GetString("js_cancel_function");

      CefRefPtr<CefMessageRouterRendererSide> router =
          CefMessageRouterRendererSide::Create(config);
      message_router_.insert(std::make_pair(config, router));
#ifdef LOG_VIA_THRIFT
      log(string_format("\t add router <%s,%s>", config.js_query_function.ToString().c_str(), config.js_cancel_function.ToString().c_str()));
#endif // LOG_VIA_THRIFT
    }
  }
}

void HelperApp::OnContextCreated(CefRefPtr<CefBrowser> browser,
                      CefRefPtr<CefFrame> frame,
                      CefRefPtr<CefV8Context> context) {
#ifdef LOG_VIA_THRIFT
  log("OnContextCreated");
#endif // LOG_VIA_THRIFT
  std::map<CefMessageRouterConfig, CefRefPtr<CefMessageRouterRendererSide>,
           cmpCfg>::iterator iter;
  for (iter = message_router_.begin(); iter != message_router_.end();
       iter++) {
    iter->second->OnContextCreated(browser, frame, context);
  }
}

void HelperApp::OnContextReleased(CefRefPtr<CefBrowser> browser,
                       CefRefPtr<CefFrame> frame,
                       CefRefPtr<CefV8Context> context) {
#ifdef LOG_VIA_THRIFT
  log("OnContextReleased");
#endif // LOG_VIA_THRIFT
  std::map<CefMessageRouterConfig, CefRefPtr<CefMessageRouterRendererSide>,
           cmpCfg>::iterator iter;
  for (iter = message_router_.begin(); iter != message_router_.end();
       iter++) {
    iter->second->OnContextReleased(browser, frame, context);
  }
}

bool HelperApp::OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                              CefRefPtr<CefFrame> frame,
                              CefProcessId source_process,
                              CefRefPtr<CefProcessMessage> message) {
#ifdef LOG_VIA_THRIFT
  log(string_format("OnProcessMessageReceived: %s", message->GetName().c_str()));
#endif // LOG_VIA_THRIFT
  if (message->GetName() == "AddMessageRouter") {
    CefRefPtr<CefListValue> args = message->GetArgumentList();
    CefMessageRouterConfig config;
    config.js_query_function = args->GetString(0);
    config.js_cancel_function = args->GetString(1);

    // only add a new message router if it wasn't already created
    if (message_router_.find(config) != message_router_.end()) {
      return true;
    }

    CefRefPtr<CefMessageRouterRendererSide> router =
        CefMessageRouterRendererSide::Create(config);
    message_router_.insert(std::make_pair(config, router));
    return true;

  } else if (message->GetName() == "RemoveMessageRouter") {
    CefRefPtr<CefListValue> args = message->GetArgumentList();
    CefMessageRouterConfig config;
    config.js_query_function = args->GetString(0);
    config.js_cancel_function = args->GetString(1);

    message_router_.erase(config);
    return true;
  }

  bool handled = false;
  std::map<CefMessageRouterConfig, CefRefPtr<CefMessageRouterRendererSide>,
           cmpCfg>::iterator iter;
  for (iter = message_router_.begin(); iter != message_router_.end();
       iter++) {
    handled = iter->second->OnProcessMessageReceived(browser, frame,
                                                     source_process, message);
    if (handled)
      break;
  }
  return handled;
}
