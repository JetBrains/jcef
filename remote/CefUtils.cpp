#include "CefUtils.h"

#include "include/cef_application_mac.h"
#include "include/cef_command_line.h"
#include "include/wrapper/cef_library_loader.h"
#include "include/cef_app.h"

#include "log/Log.h"

void preinitCef(int argc, char* argv[]) {
    Log::debug("Started CEF intialization");

    // Load the CEF framework library at runtime instead of linking directly
    // NOTE: can't load directly by custom path, getting strange errors:
    //[0314/193420.234812:ERROR:icu_util.cc(178)] icudtl.dat not found in bundle
    //[0314/193420.235447:ERROR:icu_util.cc(240)] Invalid file descriptor to ICU data received.
    std::string basePath("/Users/bocha/projects/jcef/cmake-build-debug/remote/CefServer.app/Contents/Frameworks/");
    std::string subDir = basePath + "Chromium Embedded Framework.framework/";
    std::string framework_path = subDir + "Chromium Embedded Framework";
    if (!cef_load_library(framework_path.c_str())) {
        Log::debug("Failed to load the CEF framework by path %s", framework_path.c_str());
        return;
    }

    CefMainArgs main_args(argc, argv);
    CefRefPtr<CefCommandLine> command_line =
    CefCommandLine::CreateCommandLine();
    command_line->InitFromArgv(argc, argv);

    CefSettings settings;
    settings.no_sandbox = true;
    settings.windowless_rendering_enabled = true;

    Log::debug("\tstart CefInitialize");

    CefInitialize(main_args, settings, nullptr, nullptr);

    Log::debug("CEF has been successfully initialized.");
}

std::string toString(cef_rect_t& rect) {
    return string_format("[%d,%d,%d,%d]", rect.x, rect.y, rect.width, rect.height);
}

std::string toString(cef_point_t& pt) {
    return string_format("[%d,%d]", pt.x, pt.y);
}
