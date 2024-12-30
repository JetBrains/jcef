#include "CefSettingsParser.h"

#include <fstream>
#include "Utils.h"
#include "log/Log.h"

#if defined(OS_MAC)
namespace CefUtils {
  std::string getFrameworkDir();
}
#elif defined(OS_WIN)
#include <boost/filesystem.hpp>
#endif

namespace CefSettingsParser {

bool parseSettingItem(CefSettings & out, const std::string & settingLine) {
  auto pos = settingLine.find('=', 1);
  if (pos == settingLine.npos) {
    Log::trace("Can't parse setting line: %s", settingLine.c_str());
    return false;
  }

  std::string name = settingLine.substr(0, pos);
  std::string val = settingLine.substr(pos + 1);
  //Log::trace("\t parseSetting: name=%s val=%s", name.c_str(), val.c_str());

  //
  // Fill string fields
  //
  if (name.find("browser_subprocess_path") != name.npos) {
    CefString(&out.browser_subprocess_path) = val;
  } else if (name.find("cache_path") != name.npos) {
    CefString(&out.cache_path) = val;
  } else if (name.find("user_agent") != name.npos) {
    CefString(&out.user_agent) = val;
  } else if (name.find("user_agent_product") != name.npos) {
    CefString(&out.user_agent_product) = val;
  } else if (name.find("locale") != name.npos) {
    CefString(&out.locale) = val;
  } else if (name.find("log_file") != name.npos) {
    CefString(&out.log_file) = val;
  } else if (name.find("log_severity") != name.npos) {
    if (val.find("verb") != val.npos)
      out.log_severity = LOGSEVERITY_VERBOSE;
    else if (val.find("debug") != val.npos)
      out.log_severity = LOGSEVERITY_DEBUG;
    else if (val.find("info") != val.npos)
      out.log_severity = LOGSEVERITY_INFO;
    else if (val.find("warn") != val.npos)
      out.log_severity = LOGSEVERITY_WARNING;
    else if (val.find("err") != val.npos)
      out.log_severity = LOGSEVERITY_ERROR;
    else
      out.log_severity = LOGSEVERITY_DEFAULT;
  } else if (name.find("javascript_flags") != name.npos) {
    CefString(&out.javascript_flags) = val;
  } else if (name.find("resources_dir_path") != name.npos) {
    CefString(&out.resources_dir_path) = val;
  } else if (name.find("locales_dir_path") != name.npos) {
    CefString(&out.locales_dir_path) = val;
  } else if (name.find("cookieable_schemes_list") != name.npos) {
    CefString(&out.cookieable_schemes_list) = val;
  } else if (name.find("windowless_rendering_enabled") != name.npos) {
    //
    // Fill bool fields
    //
    Log::trace("Setting 'windowless_rendering_enabled' will be ignored");
  } else if (name.find("command_line_args_disabled") != name.npos) {
    out.command_line_args_disabled = val.compare("true") == 0;
  } else if  (name.find("persist_session_cookies") != name.npos) {
    out.persist_session_cookies = val.compare("true") == 0;
  } else if (name.find("pack_loading_disabled") != name.npos) {
    out.pack_loading_disabled = val.compare("true") == 0;
  } else if (name.find("cookieable_schemes_exclude_defaults") != name.npos) {
    out.cookieable_schemes_exclude_defaults = val.compare("true") == 0;
  } else if (name.find("no_sandbox") != name.npos) {
    out.no_sandbox = val.compare("true") == 0;
  } else if (name.find("remote_debugging_port") != name.npos) {
    //
    // Fill int fields
    //
    out.remote_debugging_port = std::stoi(val);
  } else if (name.find("uncaught_exception_stack_size") != name.npos) {
    out.uncaught_exception_stack_size = std::stoi(val);
  } else if (name.find("background_color") != name.npos) {
    out.background_color = std::stoi(val);
  } else {
    Log::trace("Can't parse setting line: %s", settingLine.c_str());
    return false;
  }
  return true;
}

bool parseScheme(std::string & name, int & options, const std::string & settingLine) {
  auto pos = settingLine.find("|");
  if (pos == settingLine.npos) {
    Log::trace("Can't parse scheme line: %s", settingLine.c_str());
    return false;
  }

  name.assign(settingLine.substr(0, pos - 1));
  options = std::stoi(settingLine.substr(pos + 1));
  return true;
}

void parseSettings(const std::string & paramsFilePath, std::vector<std::string> & cmdlineSwitches/*output*/, CefSettings & settings/*output*/, std::vector<std::pair<std::string, int>> & schemes/*output*/) {
  bool collectCmdSwitches = false;
  bool collectSettings = false;
  bool collectSchemes = false;
  std::vector<std::string> parsedSettings; // just for logging
  if (!paramsFilePath.empty()) {
    std::ifstream infile(paramsFilePath);
    std::string line;
    while (std::getline(infile, line)) {
      //Log::trace("\tprocess settings line: %s", line.c_str());
      if (line.empty())
        continue;

      if (line.find("[COMMAND_LINE]:") != line.npos) {
        collectCmdSwitches = true;
        collectSettings = collectSchemes = false;
      } else if (line.find("[SETTINGS]:") != line.npos) {
        collectSettings = true;
        collectSchemes = collectCmdSwitches = false;
      } else if (line.find("[CUSTOM_SCHEMES]:") != line.npos) {
        collectSchemes = true;
        collectSettings = collectCmdSwitches = false;
      } else {
        if (!collectCmdSwitches && !collectSettings && !collectSchemes) {
          Log::warn("Parse file with params: skip unknown line %s", line.c_str());
        } else if (collectCmdSwitches) {
          cmdlineSwitches.push_back(line);
        } else if (collectSettings) {
          if (parseSettingItem(settings, line))
            parsedSettings.push_back(line);
        } else {
          std::string name;
          int options;
          if (parseScheme(name, options, line))
            schemes.push_back(std::make_pair(name, options));
        }
      }
    }
  } else
    Log::debug("Params file is empty.");

  if (Log::isTraceEnabled()) {
    Log::trace("Command line switches:");
    for (auto& sw: cmdlineSwitches)
      Log::trace("\t%s", sw.c_str());

    Log::trace("Settings:");
    for (auto& st : parsedSettings)
      Log::trace("\t%s", st.c_str());

    Log::trace("Custom schemes:");
    for (auto& sch : schemes)
      Log::trace("\t%s [%d]", sch.first.c_str(), sch.second);
  }

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
}

void fixLoggingSettings(CefSettings & settings/*output*/, int logLevel, const std::string & file) {
  const bool useSeparateLogging = getBoolEnv("CEF_SERVER_SEPARATE_LOG");
  const int customChromiumLogLevel = getLongEnv("CEF_SERVER_CHROMIUM_LOG_LEVEL", -1);
  if (customChromiumLogLevel >= 0) {
    Log::info("Setting 'log_severity' ('%d') will be replaced with custom log level '%d'", customChromiumLogLevel);
    settings.log_severity = Log::toCefLogLevel(customChromiumLogLevel);
  } else if (!useSeparateLogging && logLevel >= 0) {
    if (!Log::isEqual(logLevel, settings.log_severity))
      Log::info("Setting 'log_severity' ('%d') will be replaced with log level from command line '%d'", logLevel);
    settings.log_severity = Log::toCefLogLevel(logLevel);
  }
  
  if (!useSeparateLogging && !file.empty()) {
    const std::string logFromSettings = CefString(&settings.log_file).ToString();
    if (file.compare(logFromSettings) != 0)
      Log::info("Setting 'log_file' ('%s') will be replaced with value from command line '%s'", logFromSettings.c_str(), file.c_str());
    CefString(&settings.log_file) = file;
  }
}

} // CefSettingsParser
