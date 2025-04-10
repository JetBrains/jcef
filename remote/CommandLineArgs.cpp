#include "CommandLineArgs.h"

#include "CefSettingsParser.h"
#include "Utils.h"
#include "log/Log.h"

#if defined(OS_MAC)
namespace CefUtils {
std::string getFrameworkDir();
}
#elif defined(OS_WIN)
#include <boost/filesystem.hpp>
#endif

const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_CommandLineArgs");

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
  if (!Log::isTraceEnabled())
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

void CommandLineArgs::init(int argc, char* argv[]) {
  // This method is called very early.

  // 1. Initialize logger at first.
  for (int c = 0; c < argc; ++c) {
    const char * arg = argv[c];
    if (arg == nullptr || arg[0] == 0) continue;

    std::string word(arg);
    size_t tokenPos;
    if ((tokenPos = word.find("--logfile=")) != word.npos) {
      myPathLogFile = word.substr(tokenPos + 10);
    } else if ((tokenPos = word.find("--loglevel=")) != word.npos) {
      std::string sval = word.substr(tokenPos + 11);
      bool isNumeric = false;
      try {
        myLogLevel = std::stoi(sval);
        isNumeric = true;
      } catch (const std::exception&) {}

      if (!isNumeric) {
        std::string valLowerCase = word;
        std::transform(valLowerCase.begin(), valLowerCase.end(), valLowerCase.begin(),
                       [](unsigned char in){
                         if (in <= 'Z' && in >= 'A')
                           return in - ('Z' - 'z');
                         return (int)in;
                       });
        if (valLowerCase.find("verb") != valLowerCase.npos || valLowerCase.find("trace") != valLowerCase.npos)
          myLogLevel = LEVEL_TRACE;
        else if (valLowerCase.find("debug") != valLowerCase.npos)
          myLogLevel = LEVEL_DEBUG;
        else if (valLowerCase.find("info") != valLowerCase.npos)
          myLogLevel = LEVEL_INFO;
        else if (valLowerCase.find("warn") != valLowerCase.npos)
          myLogLevel = LEVEL_WARN;
        else if (valLowerCase.find("err") != valLowerCase.npos)
          myLogLevel = LEVEL_ERROR;
        else if (valLowerCase.find("fatal") != valLowerCase.npos)
          myLogLevel = LEVEL_FATAL;
        else if (valLowerCase.find("disable") != valLowerCase.npos)
          myLogLevel = LEVEL_DISABLED;
      }
    }
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
    } else if ((tokenPos = word.find("--params=")) != word.npos) {
      myPathParamsFile = word.substr(tokenPos + 9);
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
}

void CommandLineArgs::prepareCefSettings(CefSettings & settings) {
  for (const auto & p: myParsedCefSettings)
    CefSettingsParser::setSettingItem(settings, p.first, p.second);

  settings.windowless_rendering_enabled = true;
  settings.multi_threaded_message_loop = false;
  settings.external_message_pump = false;
  settings.no_sandbox = true; // TODO: support sandbox later.

#if defined(OS_MAC)
  CefString(&settings.framework_dir_path) = CefUtils::getFrameworkDir();
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
}
