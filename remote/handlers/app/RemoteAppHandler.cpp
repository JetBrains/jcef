#include "RemoteAppHandler.h"
#include "../../log/Log.h"

#include <fstream>

RemoteAppHandler::RemoteAppHandler(const std::vector<std::string> & switches, const CefSettings & settings, const std::vector<std::pair<std::string, int>> & schemes)
    : myArgs(switches),
      mySettings(settings),
      mySchemes(schemes),
      myBrowserProcessHandler(new RemoteBrowserProcessHandler()) {}

bool RemoteAppHandler::isDefaultRoot() const {
  return getSettingRootPath().empty();
}

std::string RemoteAppHandler::getRootPath() const {
  std::string rootFromSettings = getSettingRootPath();
  if (!rootFromSettings.empty())
    return rootFromSettings;

#if defined(OS_WIN)
  return "~\\AppData\\Local\\CEF\\User Data";
#elif defined(OS_LINUX)
  return "~/.config/cef_user_data";
#elif defined(OS_MAC)
  return "~/Library/Application Support/CEF/User Data";
#endif
}

std::string RemoteAppHandler::getSettingRootPath() const {
  /// The root directory for installation-specific data and the parent directory
  /// for profile-specific data. All CefSettings.cache_path and
  /// CefRequestContextSettings.cache_path values must have this parent
  /// directory in common. If this value is empty and CefSettings.cache_path is
  /// non-empty then it will default to the CefSettings.cache_path value. If both values are empty then
  /// the default platform-specific directory will be used
  /// ("~/.config/cef_user_data" directory on Linux, "~/Library/Application
  /// Support/CEF/User Data" directory on MacOS, "AppData\Local\CEF\User Data"
  /// directory under the user profile directory on Windows).
  ///
  /// Multiple application instances writing to the same root_cache_path
  /// directory could result in data corruption. A process singleton lock based
  /// on the root_cache_path value is therefore used to protect against this.
  /// This singleton behavior applies to all CEF-based applications using
  /// version 120 or newer. You should customize root_cache_path for your
  /// application and implement CefBrowserProcessHandler::
  /// OnAlreadyRunningAppRelaunch, which will then be called on any app relaunch
  /// with the same root_cache_path value.
  ///
  /// Failure to set the root_cache_path value correctly may result in startup
  /// crashes or other unexpected behaviors (for example, the sandbox blocking
  /// read/write access to certain files).
  ///
  std::string root = CefString(&mySettings.root_cache_path).ToString();
  if (!root.empty())
    return root;
  std::string cache = CefString(&mySettings.cache_path).ToString();
  if (!cache.empty())
    return cache;

  return "";
}

void RemoteAppHandler::OnBeforeCommandLineProcessing(
    const CefString& process_type,
    CefRefPtr<CefCommandLine> command_line
) {
  if (!process_type.empty())
    return;

  Log::debug("Original command line:\n%s", command_line->GetCommandLineString().ToString().c_str());

  std::string additionalItems;
  if (!myArgs.empty()) {
    // Copy-paste from CefAppHandlerAdapter.onBeforeCommandLineProcessing
    // Forward switches and arguments from Java to Cef
    bool parseSwitchesDone = false;
    for (const auto& arg : myArgs) {
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
          const std::string switchStr = arg.substr(switchCnt);
          const size_t eqPos = switchStr.find("=");
          if (eqPos != std::string::npos && eqPos > 0) {
            std::string s0 = switchStr.substr(0, eqPos);
            std::string s1 = switchStr.substr(eqPos + 1);
            command_line->AppendSwitchWithValue(s0, s1);
            additionalItems += s0 + "|" + s1 + ", ";
          } else {
            command_line->AppendSwitch(switchStr);
            additionalItems += switchStr + ", ";
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

    if (mySettings.cache_path.length <= 0 && !command_line->HasSwitch("disable-gpu-shader-disk-cache")) {
      // Don't create a "GPUCache" directory when cache_path is unspecified.
      command_line->AppendSwitch("disable-gpu-shader-disk-cache");
    }
  }
}

void RemoteAppHandler::OnRegisterCustomSchemes(CefRawPtr<CefSchemeRegistrar> registrar) {
  // The registered scheme has to be forwarded to all other processes which will
  // be created by the browser process (e.g. the render-process). Otherwise
  // things like JS "localStorage" get/set will end up in a crashed
  // render process.
  if (mySchemes.empty())
    return;

  std::string tmpName = utils::GetTempFile("scheme", false);
  std::ofstream fStream(tmpName.c_str(),std::ofstream::out | std::ofstream::trunc);

  Log::debug("Register custom schemes [file=%s]:", tmpName.c_str());
  for (const auto& cs: mySchemes) {
    int options = 0;
    if (cs.second & (1 << 0))
      options |= CEF_SCHEME_OPTION_STANDARD;
    if (cs.second & (1 << 1))
      options |= CEF_SCHEME_OPTION_LOCAL;
    if (cs.second & (1 << 2))
      options |= CEF_SCHEME_OPTION_DISPLAY_ISOLATED;
    if (cs.second & (1 << 3))
      options |= CEF_SCHEME_OPTION_SECURE;
    if (cs.second & (1 << 4))
      options |= CEF_SCHEME_OPTION_CORS_ENABLED;
    if (cs.second & (1 << 5))
      options |= CEF_SCHEME_OPTION_CSP_BYPASSING;
    if (cs.second & (1 << 6))
      options |= CEF_SCHEME_OPTION_FETCH_ENABLED;

    registrar->AddCustomScheme(cs.first, options);
    Log::debug("%s [%d:%d]", cs.first.c_str(), cs.second, options);

    if (fStream.is_open())
      fStream << cs.first.c_str() << "," << options;
  }

  if (fStream.is_open())
    fStream.close();

  // TODO Register file to be deleted in CefShutdown()
  // ClientApp::registerTempFile(tmpName);
}
