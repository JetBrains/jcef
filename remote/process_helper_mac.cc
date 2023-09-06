// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that can
// be found in the LICENSE file.

#include "include/cef_app.h"
#include "include/wrapper/cef_library_loader.h"
#include "include/wrapper/cef_message_router.h"

#include "./gen-cpp/Server.h"

// When generating projects with CMake the CEF_USE_SANDBOX value will be defined
// automatically. Pass -DUSE_SANDBOX=OFF to the CMake command-line to disable
// use of the sandbox.
#if defined(CEF_USE_SANDBOX)
#include "include/cef_sandbox_mac.h"
#endif

#include <thrift/transport/TSocket.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TTransportUtils.h>

#include "CefUtils.h"

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

using namespace thrift_codegen;

namespace {

// comparator to check if configuration values are the same
struct cmpCfg {
  bool operator()(const CefMessageRouterConfig& lValue,
                  const CefMessageRouterConfig& rValue) const {
    std::less<std::string> comp;
    return comp(lValue.js_query_function.ToString(),
                rValue.js_query_function.ToString());
  }
};

typedef std::unique_lock<std::recursive_mutex> Lock;

//#define LOG_VIA_THRIFT

class CefHelperApp : public CefApp, public CefRenderProcessHandler {
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
  CefHelperApp()
  {
#ifdef LOG_VIA_THRIFT
    myTransport = std::make_shared<TBufferedTransport>(std::make_shared<TSocket>("localhost", 9090));
    myService = std::make_shared<ServerClient>(std::make_shared<TBinaryProtocol>(myTransport));
    myTransport->open();
    log("Helper process connected.");
#endif // LOG_VIA_THRIFT
  }

  void OnRegisterCustomSchemes(
      CefRawPtr<CefSchemeRegistrar> registrar) override {
#ifdef LOG_VIA_THRIFT
    log("Unimplemented CefHelperApp.OnRegisterCustomSchemes");
#endif // LOG_VIA_THRIFT
    // TODO: implement
//    std::fstream fStream;
//    std::string fName = util::GetTempFileName("scheme", true);
//    char schemeName[512] = "";
//    int options;
//
//    fStream.open(fName.c_str(), std::fstream::in);
//    while (fStream.is_open() && !fStream.eof()) {
//      fStream.getline(schemeName, 512, ',');
//      if (strlen(schemeName) == 0)
//        break;
//
//      fStream >> options;
//
//      registrar->AddCustomScheme(schemeName, options);
//    }
//    fStream.close();
  }

  virtual CefRefPtr<CefRenderProcessHandler> GetRenderProcessHandler()
      override {
    return this;
  }

  void OnBrowserCreated(CefRefPtr<CefBrowser> browser,
                        CefRefPtr<CefDictionaryValue> extra_info) override {
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

  void OnContextCreated(CefRefPtr<CefBrowser> browser,
                        CefRefPtr<CefFrame> frame,
                        CefRefPtr<CefV8Context> context) override {
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

  void OnContextReleased(CefRefPtr<CefBrowser> browser,
                         CefRefPtr<CefFrame> frame,
                         CefRefPtr<CefV8Context> context) override {
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

  bool OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                                CefRefPtr<CefFrame> frame,
                                CefProcessId source_process,
                                CefRefPtr<CefProcessMessage> message) override {
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

 private:
  std::map<CefMessageRouterConfig,
           CefRefPtr<CefMessageRouterRendererSide>,
           cmpCfg>
      message_router_;

  IMPLEMENT_REFCOUNTING(CefHelperApp);
};

}  // namespace

// Entry point function for sub-processes.
int main(int argc, char* argv[]) {
#if defined(CEF_USE_SANDBOX)
  // Initialize the macOS sandbox for this helper process.
  CefScopedSandboxContext sandbox_context;
  if (!sandbox_context.Initialize(argc, argv))
    return 1;
#endif
  std::string framework_path;
  const std::string switchPrefix = "--framework-dir-path=";
  for (int i = 0; i < argc; ++i) {
    std::string arg = argv[i];
    if (arg.find(switchPrefix) == 0) {
      framework_path = arg.substr(switchPrefix.length());
      break;
    }
  }

  // Load the CEF framework library at runtime instead of linking directly
  // as required by the macOS sandbox implementation.
  CefScopedLibraryLoader library_loader;
  if (!framework_path.empty()) {
    framework_path += "/Chromium Embedded Framework";
    if (!cef_load_library(framework_path.c_str()))
      return 1;
  } else {
    return 1;
  }

  // Provide CEF with command-line arguments.
  CefMainArgs main_args(argc, argv);
  CefRefPtr<CefHelperApp> app = new CefHelperApp();

  // Execute the sub-process.
  return CefExecuteProcess(main_args, app, nullptr);
}
