#include "CefUtils.h"

#include <thread>

#include "include/cef_command_line.h"
#include "include/wrapper/cef_library_loader.h"
#include "include/cef_app.h"

#include "log/Log.h"

#include "RemoteAppHandler.h"

static bool g_isInitialized = false;

bool doLoadCefLibrary() {
    // Load the CEF framework library at runtime instead of linking directly
    // NOTE: can't load directly by custom path, getting strange errors:
    //[0314/193420.234812:ERROR:icu_util.cc(178)] icudtl.dat not found in bundle
    //[0314/193420.235447:ERROR:icu_util.cc(240)] Invalid file descriptor to ICU data received.
    std::string basePath("/Users/bocha/projects/jcef/cmake-build-debug/remote/CefServer.app/Contents/Frameworks/");
    std::string subDir = basePath + "Chromium Embedded Framework.framework/";
    std::string framework_path = subDir + "Chromium Embedded Framework";
    if (!cef_load_library(framework_path.c_str())) {
        Log::debug("Failed to load the CEF framework by path %s", framework_path.c_str());
        return false;
    }
    Log::debug("Loaded cef native library");
    return true;
}

void doCefInitializeAndRun(RemoteAppHandler * appHandler) {
    CefMainArgs main_args;
    CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();

    CefSettings settings;
    settings.no_sandbox = true;
    settings.windowless_rendering_enabled = true;

    Log::debug("Start CefInitialize");
    const bool success = CefInitialize(main_args, settings, appHandler, nullptr);
    if (!success) {
        Log::error("Cef initialization failed");
        return;
    }
    g_isInitialized = true;
    CefRunMessageLoop();
    Log::debug("Cef shutdowns");
    CefShutdown();
    Log::debug("Shutdown finished");
}

bool isCefInitialized() { return g_isInitialized; }

std::string toString(cef_rect_t& rect) {
    return string_format("[%d,%d,%d,%d]", rect.x, rect.y, rect.width, rect.height);
}

std::string toString(cef_point_t& pt) {
    return string_format("[%d,%d]", pt.x, pt.y);
}
