#include "CefUtils.h"

#include <algorithm>
#include <boost/filesystem.hpp>
#include <fstream>

#if defined(OS_MAC)
#include "include/cef_command_line.h"
#include "include/wrapper/cef_library_loader.h"
#endif

#include "log/Log.h"

#include "handlers/RemoteAppHandler.h"

namespace {
    bool g_isInitialized = false;
}

namespace CefUtils {
#if defined(OS_MAC)
    boost::filesystem::path getFrameworkPath() {
      return boost::filesystem::current_path()
          .append("..")
          .append("..")
          .append("..")
          .append("Chromium Embedded Framework.framework")
          .lexically_normal();
    }
    boost::filesystem::path getLibPath() {
      return boost::filesystem::current_path()
              .append("..")
              .append("..")
              .append("..")
              .append("Chromium Embedded Framework.framework")
              .append("Chromium Embedded Framework")
              .lexically_normal();
    }
    bool doLoadCefLibrary() {
      // Load the CEF framework library at runtime instead of linking directly
      // NOTE: can't load directly by custom libPath, getting strange errors:
      //[0314/193420.234812:ERROR:icu_util.cc(178)] icudtl.dat not found in bundle
      //[0314/193420.235447:ERROR:icu_util.cc(240)] Invalid file descriptor to ICU data received.
      // Need to put CEF into cef_server.app/Contents/Frameworks
      // TODO: fixme

      boost::filesystem::path libPath = getLibPath();
      if (!cef_load_library(libPath.c_str())) {
        Log::debug("Failed to load the CEF framework by libPath %s", libPath.c_str());
        return false;
      }
      return true;
    }
#endif

    bool isCefInitialized() { return g_isInitialized; }

    void runCefLoop() {
        setThreadName("CefMain");
        g_isInitialized = true;
        CefRunMessageLoop();
        Log::debug("Cef going shutdown.");
        CefShutdown();
        Log::debug("Shutdown finished.");
    }

    bool initializeCef(int argc, char* argv[]) {
      std::string paramsFilePath;
      if (argc > 1) {
        paramsFilePath.assign(argv[1]);
        Log::info("Use params file %s", paramsFilePath.c_str());
      } else {
        // Parse file with params
        boost::filesystem::path settingsPath =
            boost::filesystem::temp_directory_path()
                .append("cef_server_params.txt")
                .lexically_normal();
        paramsFilePath.assign(settingsPath.string());
        Log::info("Use default params file %s", paramsFilePath.c_str());
      }

      return initializeCef(paramsFilePath);
    }

    bool initializeCef(std::string paramsFilePath) {
      bool collectCmdSwitches = false;
      bool collectSettings = false;
      bool collectSchemes = false;
      std::vector<std::string> cmdlineSwitches;
      CefSettings settings;
      std::vector<std::pair<std::string, int>> schemes;
      std::vector<std::string> parsedSettings; // just for logging

      if (!paramsFilePath.empty()) {
        std::ifstream infile(paramsFilePath);
        std::string line;
        while (std::getline(infile, line)) {
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
              if (CefUtils::parseSetting(settings, line))
                parsedSettings.push_back(line);
            } else {
              std::string name;
              int options;
              if (CefUtils::parseScheme(name, options, line))
                schemes.push_back(std::make_pair(name, options));
            }
          }
        }
      }

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
      settings.log_severity = LOGSEVERITY_INFO; // TODO: remove hardcoded
      settings.no_sandbox = true; // TODO: support sandbox

#if defined(OS_MAC)
      const Clock::time_point startTime = Clock::now();
      if (!CefUtils::doLoadCefLibrary())
        return false;

      const Clock::time_point t1 = Clock::now();
      if (Log::isDebugEnabled()) {
        Duration d1 = std::chrono::duration_cast<std::chrono::microseconds>(t1 - startTime);
        Log::debug("Loaded cef library, spent %d ms", (int)d1.count()/1000);
      }
#endif
      return initializeCef(cmdlineSwitches, settings, schemes);
    }

    bool initializeCef(
        std::vector<std::string> switches,
        CefSettings settings,
        std::vector<std::pair<std::string, int>> schemes
    ) {
        CefMainArgs main_args;
#if defined(OS_MAC)
        boost::filesystem::path framework_path = getFrameworkPath();
        CefString(&settings.framework_dir_path) = framework_path.string();
#endif
#if defined(OS_WIN)
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
        RemoteAppHandler::initialize(switches, settings, schemes);
        CefRefPtr<CefApp> app = RemoteAppHandler::instance();
        Log::debug("Start CefInitialize");
        return CefInitialize(main_args, settings, app, nullptr);
    }

  bool parseSetting(CefSettings & out, const std::string & settingLine) {
    auto pos = settingLine.find('=', 1);
    if (pos == settingLine.npos) {
      Log::trace("Can't parse setting line: %s", settingLine.c_str());
      return false;
    }

    std::string name = settingLine.substr(0, pos);
    std::string val = settingLine.substr(pos + 1);

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
        // TODO: should we take into account log_level?
        Log::trace("Setting 'log_file' and 'log_level' will be ignored, log_file='%s'", val.c_str());
    } else if (name.find("log_severity") != name.npos) {
        // TODO: should we take into account log_level?
        Log::trace("Setting 'log_file' and 'log_level' will be ignored, log_file='%s'", val.c_str());
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
} // CefUtils

std::string toString(cef_rect_t& rect) {
  return string_format("[%d,%d,%d,%d]", rect.x, rect.y, rect.width, rect.height);
}

std::string toString(cef_point_t& pt) {
  return string_format("[%d,%d]", pt.x, pt.y);
}