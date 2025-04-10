#include "CefSettingsParser.h"

#include <fstream>
#include <algorithm>
#include "Utils.h"
#include "log/Log.h"

namespace {
const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_CefSettingsParser");

template <typename T>
bool stoi_safe(std::string arg, T & out) {
  try {
    out = std::stoi(arg);
    return true;
  } catch (const std::exception&) {
    Log::warn("Can't parse integer from string '%s'", arg.c_str());
  }
  return false;
}

bool parseSettingLine(const std::string & settingLine, std::vector<std::pair<std::string, std::string>> & out) {
  auto pos = settingLine.find('=', 1);
  if (pos == settingLine.npos) {
    Log::warn("Can't parse setting line: %s", settingLine.c_str());
    return false;
  }

  std::string name = settingLine.substr(0, pos);
  std::string val = settingLine.substr(pos + 1);
  if (doTrace)
    Log::trace("\t parseSettingLine: name=%s val=%s", name.c_str(), val.c_str());
  out.push_back(std::pair<std::string, std::string>(name, val));
  return true;
}

bool parseSchemeLine(const std::string & settingLine, std::string & name, int & options) {
  auto pos = settingLine.find("|");
  if (pos == settingLine.npos) {
    Log::warn("Can't parse scheme line: %s", settingLine.c_str());
    return false;
  }

  name.assign(settingLine.substr(0, pos - 1));
  stoi_safe(settingLine.substr(pos + 1), options);
  return true;
}

}


namespace CefSettingsParser {

bool setSettingItem(CefSettings & out, const std::string & name, const std::string & val) {
  //
  // Fill string fields
  //
  size_t cachedPos = name.npos;
  if (name.find("browser_subprocess_path") != name.npos) {
    CefString(&out.browser_subprocess_path) = val;
  } else if (name.find("cache_path") != name.npos) {
    CefString(&out.cache_path) = val;
  } else if (name.find("user_agent_product") != name.npos) {
    CefString(&out.user_agent_product) = val;
  } else if (name.find("user_agent") != name.npos) {
    CefString(&out.user_agent) = val;
  } else if (name.find("locales_dir_path") != name.npos) {
    CefString(&out.locales_dir_path) = val;
  } else if ((cachedPos = name.find("locale")) != name.npos && (name[cachedPos + 6] == ' ' || name[cachedPos + 6] == '=')) {
      CefString(&out.locale) = val;
  } else if (name.find("log_file") != name.npos) {
    CefString(&out.log_file) = val;
  } else if (name.find("log_severity") != name.npos) {
    std::string valLowerCase = val;
    std::transform(valLowerCase.begin(), valLowerCase.end(), valLowerCase.begin(),
                   [](unsigned char in){
                     if (in <= 'Z' && in >= 'A')
                       return in - ('Z' - 'z');
                     return (int)in;
                   });
    if (valLowerCase.find("verb") != valLowerCase.npos)
      out.log_severity = LOGSEVERITY_VERBOSE;
    else if (valLowerCase.find("debug") != valLowerCase.npos)
      out.log_severity = LOGSEVERITY_DEBUG;
    else if (valLowerCase.find("info") != valLowerCase.npos)
      out.log_severity = LOGSEVERITY_INFO;
    else if (valLowerCase.find("warn") != valLowerCase.npos)
      out.log_severity = LOGSEVERITY_WARNING;
    else if (valLowerCase.find("err") != valLowerCase.npos)
      out.log_severity = LOGSEVERITY_ERROR;
    else if (valLowerCase.find("disable") != valLowerCase.npos)
      out.log_severity = LOGSEVERITY_DISABLE;
    else
      out.log_severity = LOGSEVERITY_DEFAULT;
  } else if (name.find("javascript_flags") != name.npos) {
    CefString(&out.javascript_flags) = val;
  } else if (name.find("resources_dir_path") != name.npos) {
    CefString(&out.resources_dir_path) = val;
  } else if (name.find("cookieable_schemes_list") != name.npos) {
    CefString(&out.cookieable_schemes_list) = val;
  } else if (name.find("windowless_rendering_enabled") != name.npos) {
    //
    // Fill bool fields
    //
    if (val.compare("true") != 0)
      Log::trace("Setting 'windowless_rendering_enabled' will be ignored");
  } else if (name.find("command_line_args_disabled") != name.npos) {
    out.command_line_args_disabled = val.compare("true") == 0;
  } else if  (name.find("persist_session_cookies") != name.npos) {
    out.persist_session_cookies = val.compare("true") == 0;
  } else if (name.find("cookieable_schemes_exclude_defaults") != name.npos) {
    out.cookieable_schemes_exclude_defaults = val.compare("true") == 0;
  } else if (name.find("no_sandbox") != name.npos) {
    out.no_sandbox = val.compare("true") == 0;
  } else if (name.find("remote_debugging_port") != name.npos) {
    //
    // Fill int fields
    //
    stoi_safe(val, out.remote_debugging_port);
  } else if (name.find("uncaught_exception_stack_size") != name.npos) {
    stoi_safe(val, out.uncaught_exception_stack_size);
  } else if (name.find("background_color") != name.npos) {
    stoi_safe(val, out.background_color);
  } else {
    Log::warn("Unknown CefSetting item: %s=%s", name.c_str(), val.c_str());
    return false;
  }
  return true;
}

bool parseCefSettingWord(const std::string & arg, std::vector<std::pair<std::string, std::string>> & out) {
  auto eqPos = arg.find("=");
  auto tokenPos = arg.find("--cef_setting_");
  auto tokenShortPos = arg.find("--cs_");
  if (eqPos == arg.npos || (tokenPos == arg.npos && tokenShortPos == arg.npos)) {
    // Log::trace("Can't parse cef-setting word: %s", arg.c_str());
    return false;
  }
  std::string name = tokenPos != arg.npos ?
    arg.substr(tokenPos + 14, eqPos - 1) :
    arg.substr(tokenShortPos + 5, eqPos - 1);
  std::string val = arg.substr(eqPos + 1);
  if (doTrace)
    Log::trace("\t parseCefSettingWord: parsed name=%s val=%s", name.c_str(), val.c_str());
  out.push_back(std::pair<std::string, std::string>(name, val));
  return true;
}

bool parseCefSchemeWord(const std::string & arg, std::string & name, int & options) {
  auto eqPos = arg.find("=");
  auto tokenPos = arg.find("--cef_customscheme_");
  auto tokenShortPos = arg.find("--ccs_");
  if (eqPos == arg.npos || (tokenPos == arg.npos && tokenShortPos == arg.npos)) {
    // Log::trace("Can't parse cef-scheme word: %s", arg.c_str());
    return false;
  }
  if (tokenPos != arg.npos) {
    name.assign(arg.substr(tokenPos + 19, eqPos - 1));
  } else {
    name.assign(arg.substr(tokenShortPos + 6, eqPos - 1));
  }
  stoi_safe(arg.substr(eqPos + 1), options);
  if (doTrace)
    Log::trace("\t parseCefSchemeWord: parsed name=%s options=%d", name.c_str(), options);
  return true;
}

bool parseCefCmdLineSwitch(const std::string & arg, std::string & out) {
  auto tokenPos = arg.find("--cef_switch_");
  auto tokenShortPos = arg.find("--cw");
  if (tokenPos == arg.npos && tokenShortPos == arg.npos) {
    // Log::trace("Can't parse cef-switch word: %s", arg.c_str());
    return false;
  }

  if (tokenPos != arg.npos)
    out.assign(arg.substr(tokenPos + 13));
  else
    out.assign(arg.substr(tokenPos + 4));
  if (doTrace)
    Log::trace("\t parseCefCmdLineSwitch: parsed %s", out.c_str());
  return true;
}

void parseParamsFile(const std::string & paramsFilePath, std::vector<std::string> & cmdlineSwitches/*output*/, std::vector<std::pair<std::string, std::string>> & parsedSettings/*output*/, std::vector<std::pair<std::string, int>> & schemes/*output*/) {
  bool collectCmdSwitches = false;
  bool collectSettings = false;
  bool collectSchemes = false;
  if (!paramsFilePath.empty()) {
    std::ifstream infile(paramsFilePath);
    std::string line;
    while (std::getline(infile, line)) {
      if (doTrace)
        Log::trace("\tprocess settings line: %s", line.c_str());
      if (line.empty() || line[0] == '#')
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
          parseSettingLine(line, parsedSettings);
        } else {
          std::string name;
          int options;
          if (parseSchemeLine(line, name, options))
            schemes.push_back(std::make_pair(name, options));
        }
      }
    }
  } else
    Log::debug("Params file is empty.");
}

} // CefSettingsParser
