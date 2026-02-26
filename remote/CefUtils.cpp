#include "CefUtils.h"

#include <algorithm>
#include <boost/filesystem.hpp>
#include <fstream>

#include "ServerApplication.h"

#if defined(OS_MAC)
#include "include/wrapper/cef_library_loader.h"
#include <dirent.h>
#include <errno.h>
#endif

#include "log/Log.h"

#include "handlers/app/RemoteAppHandler.h"

#include "include/cef_version_info.h"
#include "../native/jcef_version.h"

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
    bool findFramework() {
      if (!g_pathFramework.empty())
        return true;

      // 1. check env var
      char * val = getenv("ALT_CEF_FRAMEWORK_DIR");
      if (val != NULL && isDirExist(val)) {
        std::string path = string_format("%s/%s", val, "Chromium Embedded Framework");
        if (utils::isFileExist(path.c_str())) {
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
      if (utils::isFileExist(path.c_str())) {
        g_pathFrameworkDir = path.parent_path().string();
        g_pathFramework = path.string();
        Log::trace("Will be used CEF framework from JBR bundle, path '%s'", g_pathFramework.c_str());
        return true;
      }

      // 3. check server-bundle location
      path = boost::filesystem::current_path()
                 .append("..")
                 .append("Frameworks")
                 .append("Chromium Embedded Framework.framework")
                 .append("Chromium Embedded Framework")
                 .lexically_normal();
      if (utils::isFileExist(path.c_str())) {
        g_pathFrameworkDir = path.parent_path().string();
        g_pathFramework = path.string();
        Log::trace("Will be used CEF framework from cef_server.app bundle, path '%s'", g_pathFramework.c_str());
        return true;
      }

      Log::error("Can't find CEF framework.");
      return false;
    }

    std::string getFrameworkDir() {
      findFramework();
      return g_pathFrameworkDir;
    }

    bool loadCefFramework() {
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
        CefRunMessageLoop();
        Log::debug("Cef going shutdown.");
        std::this_thread::sleep_for(std::chrono::milliseconds(100)); // sleep to prevent shutdown_checker::AssertNotShutdown() inside life_span_handler_on_before_close
        CefShutdown();
        Log::debug("Shutdown finished.");
    }

    bool initializeCef() {
      CefMainArgs main_args;
      RemoteAppHandler* app = ServerApplication::instance().getCefAppHandler();
      const bool isInitialized = CefInitialize(main_args, app->getCefSettings(), app, nullptr);
      if (isInitialized)
        Log::info("CEF is initialized, version: %s", getVersionWithSha().c_str());
      else
        Log::error("CEF initialization failed.");
      return isInitialized;

    }


  std::string getVersionWithSha() {
    return string_format("%d.%d.%d.%d_%s",
                                 cef_version_info(0),   // CEF_VERSION_MAJOR
                                 cef_version_info(1),   // CEF_VERSION_MINOR
                                 cef_version_info(2),   // CEF_VERSION_PATCH
                                 cef_version_info(3),   // CEF_COMMIT_NUMBER
                                 JCEF_COMMIT_HASH);
  }

  std::string getDefaultCachePath() {
#if defined(OS_WIN)
    return "~\\AppData\\Local\\CEF\\User Data";
#elif defined(OS_LINUX)
    return "~/.config/cef_user_data";
#elif defined(OS_MAC)
    return "~/Library/Application Support/CEF/User Data";
#endif
  }
} // CefUtils

std::string toString(cef_rect_t& rect) {
  return string_format("[%d,%d,%d,%d]", rect.x, rect.y, rect.width, rect.height);
}

std::string toString(cef_point_t& pt) {
  return string_format("[%d,%d]", pt.x, pt.y);
}