#include "CommandLineArgs.h"

#include "CefSettingsParser.h"
#include "Utils.h"
#include "log/Log.h"

#if defined(OS_MAC)
#include <boost/filesystem.hpp>
namespace CefUtils {
std::string getFrameworkDir();
}
#elif defined(OS_WIN)
#include <boost/filesystem.hpp>
#endif

namespace {
const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_CommandLineArgs");
const bool dontUseDefaultChromiumSwitches = getBoolEnv("CEF_SERVER_TRACE_DontUseDefaultChromiumSwitches");
}

CommandLineArgs::CommandLineArgs() {
  const long defVal = myOpenTransportCooldownMs;
  myOpenTransportCooldownMs = getLongEnv("CEF_SERVER_TRANSPORT_OPEN_COOLDOWN_MS", defVal);
  if (myOpenTransportCooldownMs != defVal) {
    if (myOpenTransportCooldownMs < 0) myOpenTransportCooldownMs = 0;
    if (myOpenTransportCooldownMs > 500) myOpenTransportCooldownMs = 500;
    fprintf(stderr, "\tUse OpenTransportCooldownMs=%d\n", myOpenTransportCooldownMs);
  }
}

void trace(const std::string & from, const std::vector<std::string> & cmdlineSwitches, const std::vector<std::pair<std::string, std::string>> & parsedSettings, const std::vector<std::pair<std::string, int>> & schemes) {
  if (!doTrace || !Log::isTraceEnabled())
    return;

  if (!cmdlineSwitches.empty()) {
    Log::trace("Command line switches (from %s):", from.c_str());
    for (auto& sw : cmdlineSwitches)
      Log::trace("\t%s", sw.c_str());
  }
  if (!parsedSettings.empty()) {
    Log::trace("Settings (from %s):", from.c_str());
    for (auto& st : parsedSettings)
      Log::trace("\t%s=%s", st.first.c_str(), st.second.c_str());
  }
  if (!schemes.empty()) {
    Log::trace("Custom schemes (from %s):", from.c_str());
    for (auto& sch : schemes)
      Log::trace("\t%s [%d]", sch.first.c_str(), sch.second);
  }
}

bool CommandLineArgs::init(int argc, char* argv[]) {
  // This method is called very early.

  // 1. Initialize logger at first and check '--help'
  if (doTrace)
    Log::trace("CommandLineArgs: init with args:");
  for (int c = 0; c < argc; ++c) {
    const char * arg = argv[c];
    if (arg == nullptr || arg[0] == 0) continue;

    std::string word(arg);
    size_t tokenPos;
    if (word.compare("--help") == 0 || word.compare("-h") == 0) {
      fprintf(stdout, "Usage: cef-server [--port=PORT] [--params=PARAMS_FILE] [--loglevel=LEVEL] [--logfile=FILE]\nOther switches are enumerated in CommandLineArgs.cpp\n");
      return false;
    }
    if ((tokenPos = word.find("--logfile=")) != word.npos) {
      myPathLogFile = word.substr(tokenPos + 10);
    } else if ((tokenPos = word.find("--loglevel=")) != word.npos) {
      std::string sval = word.substr(tokenPos + 11);
      try {
        myLogLevel = std::stoi(sval);
      } catch (const std::exception&) {
        myLogLevel = Log::str2level(word);
      }
    }
    if (doTrace)
      Log::trace("\t%s", word.c_str());
  } // for

  Log::init(myLogLevel, myPathLogFile);

  // 2. Parse other command line arguments.
  for (int c = 0; c < argc; ++c) {
    const char * arg = argv[c];
    if (arg == nullptr || arg[0] == 0) continue;

    // NOTE: these switches don't conflict with chromium one.
    // See https://peter.sh/experiments/chromium-command-line-switches/
    std::string word(arg);
    if (word.find("--loglevel") == 0 || word.find("--logfile") == 0)
      continue;

    if (doTrace)
      Log::trace("\tprocess cmd line arg: %s", word.c_str());

    if (word == "--cef-server-wait-debugger") {
      myWaitDebugger = true;
      continue;
    }

    std::string stmp;
    int ntmp;
    size_t tokenPos;
    if ((tokenPos = word.find("--port=")) != word.npos) {
      std::string val = word.substr(tokenPos + 7);
      myPort = std::stoi(val);
      myUseTcp = true;
    } else if ((tokenPos = word.find("--pipe=")) != word.npos) {
      myPathPipe = word.substr(tokenPos + 7);
      myUseTcp = false;
    } else if ((tokenPos = word.find("--params=")) != word.npos) {
      myPathParamsFile = word.substr(tokenPos + 9);
    } else if ((tokenPos = word.find("--root=")) != word.npos) {
      myPathRootCache = word.substr(tokenPos + 7);
    } else if ((tokenPos = word.find("--logchromiumfile=")) != word.npos) {
      myPathChromiumLogFile = word.substr(tokenPos + 18);
    } else if ((tokenPos = word.find("--logchromiumlevel=")) != word.npos) {
      std::string sval = word.substr(tokenPos + 19);
      try {
        myLogLevelChromium = std::stoi(sval);
      } catch (const std::exception&) {}
    } else if ((tokenPos = word.find("--deleteRootCacheDir")) != word.npos) {
      myDeleteRootCacheDir = true;
    } else if (CefSettingsParser::parseCefCmdLineSwitch(word, stmp)) {
      myChromiumSwitches.push_back(stmp);
    } else if (CefSettingsParser::parseCefSchemeWord(word, stmp, ntmp)) {
      myCustomSchemes.push_back(std::make_pair(stmp, ntmp));
    } else if (CefSettingsParser::parseCefSettingWord(word, myParsedCefSettings)) {
      ; // nothing to do
    } else if (word.find("--") == 0) {
      myChromiumSwitches.push_back(word);
    } else {
      if (doTrace)
        Log::trace("Parse command line: skip unknown word %s", word.c_str());
    }
  } // for

  trace("command line", myChromiumSwitches, myParsedCefSettings, myCustomSchemes);

  if (!myPathParamsFile.empty()) {
    std::vector<std::pair<std::string, std::string>> fileSettings;
    std::vector<std::string> fileSwitches;
    std::vector<std::pair<std::string, int>> fileSchemes;
    CefSettingsParser::parseParamsFile(myPathParamsFile, fileSwitches, fileSettings, fileSchemes);
    if (!fileSwitches.empty() || !fileSettings.empty() || !fileSchemes.empty()) {
      Log::debug("Params file isn't empty, some command line arguments can be overriden.");
      trace("file", fileSwitches, fileSettings, fileSchemes);
      myChromiumSwitches.insert(myChromiumSwitches.end(), fileSwitches.begin(), fileSwitches.end());
      myParsedCefSettings.insert(myParsedCefSettings.end(), fileSettings.begin(), fileSettings.end());
      myCustomSchemes.insert(myCustomSchemes.end(), fileSchemes.begin(), fileSchemes.end());
    }
  }

  // Init default chromium switches and CefSettings (if necessary)
  if (myChromiumSwitches.empty() && !dontUseDefaultChromiumSwitches) { // NOTE: dontUseDefaultChromiumSwitches == false by default
    Log::debug("Use default chromium switches.");
#if defined(OS_WIN)
    // TODO: implement
#elif defined(OS_MAC)
    myChromiumSwitches.push_back("--disable-in-process-stack-traces");
    myChromiumSwitches.push_back("--use-mock-keychain");
    myChromiumSwitches.push_back("--disable-features=SpareRendererForSitePerProcess");
    myChromiumSwitches.push_back("--disable-notifications");
    myChromiumSwitches.push_back("--disable-gpu-process-crash-limit");
    myChromiumSwitches.push_back("--autoplay-policy=no-user-gesture-required");
    myChromiumSwitches.push_back("--disable-component-update");
  #else
    // OS_LINUX
    // TODO: implement
  #endif
  } // myChromiumSwitches.empty()

  if (myParsedCefSettings.empty()) {
    Log::debug("Use default cef settings.");
    myParsedCefSettings.push_back(std::make_pair("log_severity", Log::cefLogLevel2str(myLogLevelChromium)));
    myParsedCefSettings.push_back(std::make_pair("no_sandbox", "true"));
    if (!myPathChromiumLogFile.empty())
      myParsedCefSettings.push_back(std::make_pair("log_file", myPathChromiumLogFile));
    myParsedCefSettings.push_back(std::make_pair("windowless_rendering_enabled", "true"));

#if defined(OS_WIN)
    // TODO: implement
#elif defined(OS_MAC)
#else
    // OS_LINUX
    // TODO: implement
#endif
  }

  trace("merged", myChromiumSwitches, myParsedCefSettings, myCustomSchemes);
  return true;
}

void CommandLineArgs::prepareCefSettings(void * pCefSettings) {
  CefSettings & settings = *reinterpret_cast<CefSettings*>(pCefSettings);
  for (const auto & p: myParsedCefSettings) {
    if (p.first.compare("cache_path") && !myPathRootCache.empty()) {
      Log::debug("Setting 'cache_path' from params file (or cmd line) with value '%s' will be overrriden with cmd line arg '--root=%s'", p.second.c_str(), myPathRootCache.c_str());
      CefString(&settings.cache_path) = myPathRootCache;
    } else
      CefSettingsParser::setSettingItem(settings, p.first, p.second);
  }

  settings.windowless_rendering_enabled = true;
  settings.multi_threaded_message_loop = false;
  settings.external_message_pump = false;
  settings.no_sandbox = true; // TODO: support sandbox later.

#if defined(OS_MAC)
  CefString(&settings.framework_dir_path) = CefUtils::getFrameworkDir();
  if (settings.browser_subprocess_path.length == 0) {
    // example: browser_subprocess_path=/Users/bocha/Downloads/jbr_jcef-25.0.1-osx-aarch64-b245.32/Contents/Frameworks/cef_server.app/Contents/Frameworks/cef_server Helper.app/Contents/MacOS/cef_server Helper
    boost::filesystem::path path = boost::filesystem::current_path()
                                       .append("..")
                                       .append("Frameworks")
                                       .append("cef_server Helper.app")
                                       .append("Contents")
                                       .append("MacOS")
                                       .append("cef_server Helper")
                                       .lexically_normal();
    if (utils::isFileExist(path.c_str())) {
      Log::debug("Will be used browser_subprocess_path '%s'", path.c_str());
      CefString(&settings.browser_subprocess_path) = path.string();
    } else
      Log::error("Empty browser_subprocess_path.");
  }
#elif defined(OS_WIN)
  auto installation_root =
      boost::filesystem::current_path().append("..").lexically_normal();

  boost::filesystem::path resources_dir_path =
      installation_root.append("lib");
  boost::filesystem::path framework_dir_path =
      installation_root.append("bin");

  std::string resources_path = resources_dir_path.string();
  std::string locales_dir_path =
      resources_dir_path.append("locales").string();

  CefString(&settings.resources_dir_path).FromString(resources_path);
  CefString(&settings.locales_dir_path).FromString(locales_dir_path);
#endif

#if defined(OS_POSIX)
  settings.disable_signal_handlers = true;
#endif

  if (Log::isTraceEnabled()) {
    std::stringstream ss;
    ss << "browser_subprocess_path=" << CefString(&settings.browser_subprocess_path).c_str() << std::endl;
    ss << "cache_path=" << CefString(&settings.cache_path).c_str() << std::endl;
    ss << "user_agent_product=" << CefString(&settings.user_agent_product).c_str() << std::endl;
    ss << "user_agent=" << CefString(&settings.user_agent).c_str() << std::endl;
    ss << "locales_dir_path=" << CefString(&settings.locales_dir_path).c_str() << std::endl;
    ss << "locale=" << CefString(&settings.locale).c_str() << std::endl;
    ss << "log_file=" << CefString(&settings.log_file).c_str() << std::endl;
    ss << "log_severity=" << settings.log_severity << std::endl;
    ss << "javascript_flags=" << CefString(&settings.javascript_flags).c_str() << std::endl;
    ss << "resources_dir_path=" << CefString(&settings.resources_dir_path).c_str() << std::endl;
    ss << "cookieable_schemes_list=" << CefString(&settings.cookieable_schemes_list).c_str() << std::endl;
    ss << "windowless_rendering_enabled=" << settings.windowless_rendering_enabled << std::endl;
    ss << "command_line_args_disabled=" << settings.command_line_args_disabled << std::endl;
    ss << "persist_session_cookies=" << settings.persist_session_cookies << std::endl;
    ss << "cookieable_schemes_exclude_defaults=" << settings.cookieable_schemes_exclude_defaults << std::endl;
    ss << "no_sandbox=" << settings.no_sandbox << std::endl;
    ss << "remote_debugging_port=" << settings.remote_debugging_port << std::endl;
    ss << "uncaught_exception_stack_size=" << settings.uncaught_exception_stack_size << std::endl;
    ss << "background_color=" << settings.background_color << std::endl;
    Log::trace("Prepared CefSettings:\n%s", ss.str().c_str());
  }
}
