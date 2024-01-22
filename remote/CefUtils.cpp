#include "CefUtils.h"

#include <thread>
#include <algorithm>
#include <boost/filesystem.hpp>

// TODO(kharitonov): get gid of boost here
#if defined(OS_MAC)
#include <boost/filesystem.hpp>

#include "include/cef_command_line.h"
#include "include/wrapper/cef_library_loader.h"
#endif

#include "log/Log.h"

#include "handlers/RemoteAppHandler.h"

namespace {
    bool g_isInitialized = false;
    void fillSettings(CefSettings & out, const std::map<std::string, std::string>& settings);
}

namespace CefUtils {
#if defined(OS_MAC)
    bool doLoadCefLibrary() {
      // Load the CEF framework library at runtime instead of linking directly
      // NOTE: can't load directly by custom libPath, getting strange errors:
      //[0314/193420.234812:ERROR:icu_util.cc(178)] icudtl.dat not found in bundle
      //[0314/193420.235447:ERROR:icu_util.cc(240)] Invalid file descriptor to ICU data received.
      // Need to put CEF into cef_server.app/Contents/Frameworks
      // TODO: fixme

      boost::filesystem::path libPath =
          boost::filesystem::current_path()
              .append("..")
              .append("..")
              .append("..")
              .append("Chromium Embedded Framework.framework")
              .append("Chromium Embedded Framework")
              .lexically_normal();
      if (!cef_load_library(libPath.c_str())) {
        Log::debug("Failed to load the CEF framework by libPath %s", libPath.c_str());
        return false;
      }
      Log::debug("Loaded cef native library");
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

    bool initializeCef() {
        CefMainArgs main_args;
        CefSettings cefSettings;
        fillSettings(cefSettings, RemoteAppHandler::instance().getSettings());
#if defined(OS_MAC)
        boost::filesystem::path framework_path = boost::filesystem::current_path().append("..").append("..").append("..").append("./Chromium Embedded Framework.framework").lexically_normal();
        CefString(&cefSettings.framework_dir_path) = framework_path.string();
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

        CefString(&cefSettings.resources_dir_path).FromString(resources_path);
        CefString(&cefSettings.locales_dir_path).FromString(locales_dir_path);
#endif
        Log::debug("Start CefInitialize");
        CefRefPtr<CefApp> app = &RemoteAppHandler::instance();
        return CefInitialize(main_args, cefSettings, app, nullptr);
    }
} // CefUtils

std::string toString(cef_rect_t& rect) {
    return string_format("[%d,%d,%d,%d]", rect.x, rect.y, rect.width, rect.height);
}

std::string toString(cef_point_t& pt) {
    return string_format("[%d,%d]", pt.x, pt.y);
}

namespace {
    void fillSettings(CefSettings & out, const std::map<std::string, std::string>& settings) {
        CefString tmp;
        //
        // Fill string fields
        //
        if (settings.count("browser_subprocess_path") > 0) {
            CefString(&out.browser_subprocess_path) = tmp;
            tmp.clear();
        }
        if (settings.count("cache_path") > 0) {
            CefString(&out.cache_path) = tmp;
            tmp.clear();
        }
        if (settings.count("user_agent") > 0) {
            CefString(&out.user_agent) = tmp;
            tmp.clear();
        }
        if (settings.count("user_agent_product") > 0) {
            CefString(&out.user_agent_product) = tmp;
            tmp.clear();
        }
        if (settings.count("locale") > 0) {
            CefString(&out.locale) = tmp;
            tmp.clear();
        }
        if (settings.count("log_file") > 0) {
            // TODO: should we take into account log_level?
            Log::debug("Setting 'log_file' and 'log_level' will be ignored, log_file='%s'", settings.at("log_file").c_str());
        }
        if (settings.count("javascript_flags") > 0) {
            CefString(&out.javascript_flags) = tmp;
            tmp.clear();
        }
        if (settings.count("resources_dir_path") > 0) {
            CefString(&out.resources_dir_path) = tmp;
            tmp.clear();
        }
        if (settings.count("locales_dir_path") > 0) {
            CefString(&out.locales_dir_path) = tmp;
            tmp.clear();
        }
        if (settings.count("cookieable_schemes_list") > 0) {
            CefString(&out.cookieable_schemes_list) = tmp;
            tmp.clear();
        }

        //
        // Fill bool fields
        //

        if (settings.count("windowless_rendering_enabled") > 0) {
            std::string val = settings.at("windowless_rendering_enabled");
            if (val.compare("true") != 0)
              Log::debug("Setting 'windowless_rendering_enabled==false' will be ignored");
        }
        out.windowless_rendering_enabled = true;
        if (settings.count("command_line_args_disabled") > 0) {
            out.command_line_args_disabled = settings.at("command_line_args_disabled").compare("true") == 0;
        }
        if (settings.count("persist_session_cookies") > 0) {
            out.persist_session_cookies = settings.at("persist_session_cookies").compare("true") == 0;
        }
        if (settings.count("pack_loading_disabled") > 0) {
            out.pack_loading_disabled = settings.at("pack_loading_disabled").compare("true") == 0;
        }
        if (settings.count("cookieable_schemes_exclude_defaults") > 0) {
            out.cookieable_schemes_exclude_defaults = settings.at("cookieable_schemes_exclude_defaults").compare("true") == 0;
        }
        if (settings.count("no_sandbox") > 0) {
            out.no_sandbox = settings.at("no_sandbox").compare("true") == 0;
        }

        //
        // Fill int fields
        //

        if (settings.count("remote_debugging_port") > 0) {
            out.remote_debugging_port = std::stoi(settings.at("remote_debugging_port"));
        }
        if (settings.count("uncaught_exception_stack_size") > 0) {
            out.uncaught_exception_stack_size = std::stoi(settings.at("uncaught_exception_stack_size"));
        }
        if (settings.count("background_color") > 0) {
            out.background_color = std::stoi(settings.at("background_color"));
        }

        // TODO: should we take into account log_level?
        out.log_severity = LOGSEVERITY_INFO;

        // TODO: support sandbox
        out.no_sandbox = true;
    }
} // namespace