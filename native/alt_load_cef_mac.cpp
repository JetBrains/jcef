#include <string>
#include "include/wrapper/cef_library_loader.h"
#include "alt_load_cef_mac.h"

/**
 * [tav] Used instead of CefScopedLibraryLoader::Load to provide alternative framework path
 */
bool AltLoadCefLibrary() {
    static const char rel_framework_dir[] = "Frameworks/Chromium Embedded Framework.framework";
    static const char framework_name[] = "Chromium Embedded Framework";

    const char* alt_cef_framework_dir_env = getenv(ALT_CEF_FRAMEWORK_DIR);
    std::string alt_cef_framework_dir;

    if (alt_cef_framework_dir_env == nullptr) {
      // JBR_HOME is inherited by jcef_helper process from the main process
      const char* jbr_home = getenv(JBR_HOME);
      if (jbr_home == nullptr) {
        fprintf(stderr, "JBR_HOME env var is not set.\n");
        return false;
      }
      alt_cef_framework_dir = std::string(jbr_home) + "/../" + rel_framework_dir;
    }
    else {
      alt_cef_framework_dir = std::string(alt_cef_framework_dir_env);
    }

    const std::string& abs_cef_framework_path = alt_cef_framework_dir + "/" + framework_name;

    if (abs_cef_framework_path.empty()) {
        fprintf(stderr, "App does not have the expected bundle structure.\n");
        return false;
    }

    // Load the CEF framework library.
    if (!cef_load_library(abs_cef_framework_path.c_str())) {
        fprintf(stderr, "Failed to load the CEF framework.\n");
        return false;
    }

    setenv(ALT_CEF_FRAMEWORK_DIR, alt_cef_framework_dir.c_str(), 0);
    return true;
}
