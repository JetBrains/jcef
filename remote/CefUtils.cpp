#include "CefUtils.h"

#include <algorithm>
#include <boost/filesystem.hpp>
#include <fstream>

#include "ServerState.h"

#if defined(OS_MAC)
#include "include/wrapper/cef_library_loader.h"
#include <dirent.h>
#include <errno.h>
#endif

#if defined(OS_LINUX)
#include <X11/Xlib.h>
#endif

#include "log/Log.h"

#include "handlers/app/RemoteAppHandler.h"

namespace CefUtils {
#if defined(OS_MAC)
    std::string g_pathFrameworkDir = "";
    std::string g_pathFramework = "";
    bool isDirExist(const char* pathname) {
      DIR* dir = opendir(pathname);
      if (dir) {
        closedir(dir);
        return true;
      }
      if (ENOENT != errno)
        Log::error("opendir() failed, err=%d", errno);
      return false;
    }
    bool isFileExist(const char* pathname) {
      if (FILE *file = fopen(pathname, "r")) {
        fclose(file);
        return true;
      }
      return false;
    }
    bool findFramework() {
      if (!g_pathFramework.empty())
        return true;

      // 1. check env var
      char * val = getenv("ALT_CEF_FRAMEWORK_DIR");
      if (val != NULL && isDirExist(val)) {
        std::string path = string_format("%s/%s", val, "Chromium Embedded Framework");
        if (isFileExist(path.c_str())) {
          g_pathFrameworkDir = val;
          g_pathFramework = path;
          Log::debug("Will be used alt CEF framework path '%s'", g_pathFramework.c_str());
          return true;
        }
        Log::warn("Alt CEF framework path '%s' doesn't contain subfolder 'Chromium Embedded Framework'", g_pathFramework.c_str());
      }

     // 2. check JBR-case location
      boost::filesystem::path path = boost::filesystem::current_path()
                                         .append("..")
                                         .append("..")
                                         .append("..")
                                         .append("Chromium Embedded Framework.framework")
                                         .append("Chromium Embedded Framework")
                                         .lexically_normal();
      if (isFileExist(path.c_str())) {
        g_pathFrameworkDir = boost::filesystem::current_path()
                                 .append("..")
                                 .append("..")
                                 .append("..")
                                 .append("Chromium Embedded Framework.framework")
                                 .lexically_normal().string();
        g_pathFramework = path.string();
        Log::trace("Will be used CEF framework from JBR, path '%s'", g_pathFramework.c_str());
        return true;
      }

      // 3. check server-bundle location
      path = boost::filesystem::current_path()
                 .append("..")
                 .append("Frameworks")
                 .append("Chromium Embedded Framework.framework")
                 .append("Chromium Embedded Framework")
                 .lexically_normal();
      if (isFileExist(path.c_str())) {
        g_pathFrameworkDir = boost::filesystem::current_path()
                                 .append("..")
                                 .append("Frameworks")
                                 .append("Chromium Embedded Framework.framework")
                                 .lexically_normal().string();
        g_pathFramework = path.string();
        Log::trace("Will be used CEF framework from bundle, path '%s'", g_pathFramework.c_str());
        return true;
      }

      Log::error("Can't find CEF framework.");
      return false;
    }

    bool doLoadCefLibrary() {
      if (!findFramework())
        return false;
      if (!cef_load_library(g_pathFramework.c_str())) {
        Log::debug("Failed to load the CEF framework by path %s", g_pathFramework.c_str());
        return false;
      }
      return true;
    }
#endif

    void runCefLoop() {
        setThreadName("CefMain");
        CefRunMessageLoop();
        Log::debug("Cef going shutdown.");
        CefShutdown();
        Log::debug("Shutdown finished.");
    }

    bool initializeCef(
        std::vector<std::string> switches,
        CefSettings& settings,
        std::vector<std::pair<std::string, int>> schemes
    ) {
        CefMainArgs main_args;
#if defined(OS_MAC)
        CefString(&settings.framework_dir_path) = g_pathFrameworkDir;
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
#elif defined(OS_LINUX)
        XInitThreads();
#endif
        RemoteAppHandler::initialize(switches, settings, schemes);
        CefRefPtr<CefApp> app = RemoteAppHandler::instance();
        Log::debug("Start CefInitialize");
        return CefInitialize(main_args, settings, app, nullptr);
    }

    bool initializeCef() {
#if defined(OS_MAC)
      const Clock::time_point startTime = Clock::now();
      if (!doLoadCefLibrary())
        return false;

      const Clock::time_point t1 = Clock::now();
      if (Log::isDebugEnabled()) {
        Duration d1 = std::chrono::duration_cast<std::chrono::microseconds>(t1 - startTime);
        Log::debug("Loaded cef library, spent %d ms", (int)d1.count()/1000);
      }
#endif
      const CommandLineArgs& cmdArgs = ServerState::instance().getCmdArgs();
      std::string paramsFilePath = cmdArgs.getParamsFile();
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

      if (cmdArgs.getLogLevel() >= 0) {
        if (!Log::isEqual(cmdArgs.getLogLevel(), settings.log_severity))
          Log::trace("Setting 'log_severity' ('%d') will be replaced with log level from command line '%d'", cmdArgs.getLogLevel());
        settings.log_severity = Log::toCefLogLevel(cmdArgs.getLogLevel());
      }

      if (!cmdArgs.getLogFile().empty()) {
        const std::string logFromSettings = CefString(&settings.log_file).ToString();
        if (cmdArgs.getLogFile() != logFromSettings)
          Log::trace("Setting 'log_file' ('%s') will be replaced with value from command line '%s'", logFromSettings.c_str(), cmdArgs.getLogFile().c_str());
      }

      return initializeCef(cmdlineSwitches, settings, schemes);
    }

  bool parseSetting(CefSettings & out, const std::string & settingLine) {
    auto pos = settingLine.find('=', 1);
    if (pos == settingLine.npos) {
      Log::trace("Can't parse setting line: %s", settingLine.c_str());
      return false;
    }

    std::string name = settingLine.substr(0, pos);
    std::string val = settingLine.substr(pos + 1);
    std::transform(val.begin(), val.end(), val.begin(),
                   [](unsigned char c){ return std::tolower(c); });
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
} // CefUtils

std::string toString(cef_rect_t& rect) {
  return string_format("[%d,%d,%d,%d]", rect.x, rect.y, rect.width, rect.height);
}

std::string toString(cef_point_t& pt) {
  return string_format("[%d,%d]", pt.x, pt.y);
}