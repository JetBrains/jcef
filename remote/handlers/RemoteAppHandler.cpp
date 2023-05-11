#include "RemoteAppHandler.h"
#include "../log/Log.h"

using namespace thrift_codegen;

RemoteAppHandler::RemoteAppHandler(
    std::shared_ptr<BackwardConnection> backwardConnection,
    const std::vector<std::string> & args,
    const std::map<std::string, std::string>& settings
) : RpcExecutor(backwardConnection),
    myArgs(args),
    mySettings(settings),
    myBrowserProcessHandler(new RemoteBrowserProcessHandler(backwardConnection)) {}

void RemoteAppHandler::OnBeforeCommandLineProcessing(
    const CefString& process_type,
    CefRefPtr<CefCommandLine> command_line) {
  LNDCT();
  if (!process_type.empty())
    return;

  Log::debug("Original command line:\n%s", command_line->GetCommandLineString().ToString().c_str());

  std::string additionalItems = "";
  if (!myArgs.empty()) {
    // Copy-paste from CefAppHandlerAdapter.onBeforeCommandLineProcessing
    // Forward switches and arguments from Java to Cef
    bool parseSwitchesDone = false;
    for (auto arg : myArgs) {
      if (parseSwitchesDone || arg.length() < 2) {
        command_line->AppendArgument(arg);
        additionalItems += arg + ", ";
        continue;
      }
      // Arguments with '--', '-' and, on Windows, '/' prefixes are considered switches.
      int switchCnt = (arg.find("--") == 0)
                          ? 2
                          : (arg.find("/") == 0) ? 1 : (arg.find("-") == 0) ? 1 : 0;
      switch (switchCnt) {
        case 2:
          // An argument of "--" will terminate switch parsing with all subsequent
          // tokens
          if (arg.length() == 2) {
            parseSwitchesDone = true;
            continue;
          }
        // FALL THRU
        case 1: {
          // Switches can optionally have a value specified using the '=' delimiter
          // (e.g. "-switch=value").
          int eqPos = arg.find("=");
          std::string s0 = arg.substr(switchCnt, eqPos);
          if (eqPos > 0) {
            std::string s1 = arg.substr(eqPos + 1);
            command_line->AppendSwitchWithValue(s0, s1);
            additionalItems += s0 + "|" + s1 + ", ";
          } else {
            command_line->AppendSwitch(s0);
            additionalItems += s0 + ", ";
          }
          break;
        }
        case 0:
          command_line->AppendArgument(arg);
          additionalItems += arg + ", ";
          break;
      }
    }
  }

  Log::debug("Additional command line items:\n%s", additionalItems.c_str());

  // Copy-paste from ClientApp::OnBeforeCommandLineProcessing
  if (process_type.empty()) {
#if defined(OS_MAC)
    // If windowed rendering is used, we need the browser window as CALayer
    // due Java7 is CALayer based instead of NSLayer based.
    command_line->AppendSwitch("use-core-animation");

    // Skip keychain prompt on startup.
    command_line->AppendSwitch("use-mock-keychain");
#endif  // defined(OS_MAC)

    if ((mySettings.count("cache_path") == 0 || mySettings.at("cache_path").empty())
        && !command_line->HasSwitch("disable-gpu-shader-disk-cache")) {
      // Don't create a "GPUCache" directory when cache_path is unspecified.
      command_line->AppendSwitch("disable-gpu-shader-disk-cache");
    }
  }
}

void RemoteAppHandler::OnRegisterCustomSchemes(
    CefRawPtr<CefSchemeRegistrar> registrar) {
  LNDCT();
  std::vector<CustomScheme> result;
  exec([&](RpcExecutor::Service s){
    s->getRegisteredCustomSchemes(result);
  });

  Log::debug("Additional schemes:");
  for (auto cs: result) {
    int options = 0;
    if (cs.options & (1 << 0))
      options |= CEF_SCHEME_OPTION_STANDARD;
    if (cs.options & (1 << 1))
      options |= CEF_SCHEME_OPTION_LOCAL;
    if (cs.options & (1 << 2))
      options |= CEF_SCHEME_OPTION_DISPLAY_ISOLATED;
    if (cs.options & (1 << 3))
      options |= CEF_SCHEME_OPTION_SECURE;
    if (cs.options & (1 << 4))
      options |= CEF_SCHEME_OPTION_CORS_ENABLED;
    if (cs.options & (1 << 5))
      options |= CEF_SCHEME_OPTION_CSP_BYPASSING;
    if (cs.options & (1 << 6))
      options |= CEF_SCHEME_OPTION_FETCH_ENABLED;

    registrar->AddCustomScheme(cs.schemeName, options);
    Log::debug("%s [%d:%d]", cs.schemeName.c_str(), cs.options, options);
  }
}

CefRefPtr<CefBrowserProcessHandler> RemoteAppHandler::GetBrowserProcessHandler() {
  return myBrowserProcessHandler;
}
