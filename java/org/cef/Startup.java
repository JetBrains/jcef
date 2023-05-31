package org.cef;

import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

class Startup {
    private static final String ALT_CEF_FRAMEWORK_DIR = Utils.getString("ALT_CEF_FRAMEWORK_DIR");
    private static final String ALT_CEF_HELPER_APP_DIR = Utils.getString("ALT_CEF_HELPER_APP_DIR");
    private static final String ALT_JCEF_LIB_DIR = Utils.getString("ALT_JCEF_LIB_DIR");

    private static final int STARTUP_TEST_DELAY_MS = Utils.getInteger("jcef_app_startup_test_delay_ms", 0);

    private static CompletableFuture<Boolean> futureStartup = null;
    private static String tmpLibDir = null;

    static void thenAccept(Consumer<? super Boolean> action) {
        futureStartup.thenAccept(action);
    }

    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    private static boolean unpackFromJar(String resource, String outDir) throws IOException {
        CefLog.Debug("Unpack from jar, resource: %s, outDir: %s", resource, outDir);
        long startMs = System.currentTimeMillis();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null)
            classLoader = CefApp.class.getClassLoader();
        InputStream input = classLoader.getResourceAsStream(resource);
        if (input == null) {
            CefLog.Error("Can't getResourceAsStream %s from jar %s", resource, CefApp.class.getResource("CefApp.class").toString());
            return false;
        }
        String path = outDir + "/" + resource;
        FileOutputStream out = new FileOutputStream(path);
        copyStream(input, out);
        out.close();
        CefLog.Debug("Spent %d ms", System.currentTimeMillis() - startMs);
        return true;
    }

    private static void loadCEF() {
        if (OS.isMacintosh()) {
            // NOTE: In OSX framework is loaded inside N_Startup
            CefLog.Debug("loadCEF() shouldn't be called in OSX.");
            return;
        }

        if (ALT_CEF_FRAMEWORK_DIR != null && !ALT_CEF_FRAMEWORK_DIR.isEmpty()) {
            String libname = OS.isLinux() ? "libcef.so" : "libcef.dll";
            String pathToCef = ALT_CEF_FRAMEWORK_DIR + "/" + libname;
            CefLog.Info("Load CEF by path '%s'", pathToCef);
            if (OS.isLinux()) {
                System.load(pathToCef);
            } else {
                System.load(ALT_CEF_FRAMEWORK_DIR + "/chrome_elf.dll");
                System.load(pathToCef);
            }
        } else {
            if (OS.isLinux()) {
                SystemBootstrap.loadLibrary("cef");
            } else {
                SystemBootstrap.loadLibrary("chrome_elf");
                SystemBootstrap.loadLibrary("libcef");
            }
        }
    }

    private static boolean loadJcefFromJar() {
        // Use libjcef from jar (or custom location).
        tmpLibDir = null;
        final String libjcef = getLibJcefName();
        URL url = CefApp.class.getResource("CefApp.class");
        if (url == null || !url.toString().contains(".jar")) {
            // CefApp wasn't loaded from jar (for example from compiled classes), so try load
            // libjcef from custom location (for example, for debugging).
            if (ALT_JCEF_LIB_DIR == null || ALT_JCEF_LIB_DIR.isEmpty()) {
                CefLog.Error("Can't locate libjcef.");
                return false;
            }
            tmpLibDir = ALT_JCEF_LIB_DIR;
            CefLog.Info("Load %s from dir %s.", libjcef, tmpLibDir);
        } else {
            // Check that already was extracted (unpack if necessary).
            tmpLibDir = System.getProperty("java.io.tmpdir");
            File tmpLib = new File(tmpLibDir + "/" + libjcef);
            if (tmpLib.exists() && tmpLib.isFile()) {
                CefLog.Info("Use previously extracted %s in dir: %s", libjcef, tmpLibDir);
            } else {
                CefLog.Info("Extract native lib into temp dir: " + tmpLibDir);
                try {
                    boolean success = unpackFromJar(libjcef, tmpLibDir);
                    if (success) {
                        if (OS.isLinux())
                            unpackFromJar("jcef_helper", tmpLibDir);
                        else if (OS.isMacintosh()) {
                            unpackFromJar("helpers.tar.gz", tmpLibDir);
                            String unpackCmd = "tar -C " + tmpLibDir + " -xzf " + tmpLibDir + "/helpers.tar.gz";
                            CefLog.Debug("Unpack heplers, cmd: %s", unpackCmd);
                            Runtime.getRuntime().exec(unpackCmd);
                        } else if (OS.isWindows()) {
                            unpackFromJar("jcef_helper.exe", tmpLibDir);
                        }
                    }
                } catch (IOException e) {
                    CefLog.Error("Can't unpack binary from jar. Error: %s", e.getMessage());
                    tmpLibDir = null;
                    return false;
                }
            }
        }

        String libPath = tmpLibDir + "/" + libjcef;
        try {
            System.load(libPath);
        } catch (Throwable e) {
            CefLog.Error("Shared jcef library wasn't loaded, path: '%s'. Error: %s", libPath, e.getMessage());
            tmpLibDir = null;
            return false;
        }

        return true;
    }

    private static String getLibJcefName() {
        return OS.isLinux() ? "libjcef.so" : (OS.isMacintosh() ? "libjcef.dylib" : "jcef.dll");
    }

    static boolean startLoading(Function<String, Boolean> nativeStartup) {
        futureStartup = new CompletableFuture<>();

        Runnable r = () -> {
            testSleep(STARTUP_TEST_DELAY_MS);
            if (OS.isWindows()) {
                // [tav] "jawt" is loaded by JDK AccessBridgeLoader that leads to UnsatisfiedLinkError
                try {
                    SystemBootstrap.loadLibrary("jawt");
                } catch (UnsatisfiedLinkError e) {
                    CefLog.Error("Can't load jawt library, error: " + e.getMessage());
                }
                try {
                    loadCEF();
                } catch (Throwable e) {
                    CefLog.Error("Can't load CEF library, error: " + e.getMessage());
                    futureStartup.complete(false);
                    return;
                }
            }

            try {
                // At first check libjcef is in jbr.
                boolean loaded = false;
                try {
                    SystemBootstrap.loadLibrary("jcef");
                    loaded = true;
                } catch (Throwable e) {
                    CefLog.Info("Shared jcef library isn't bundled with runtime (error: %s). Will be used %s from jcef.jar", e.getMessage(), getLibJcefName());
                }

                // Try load jcef library from alt locations.
                if (!loaded && !loadJcefFromJar()) {
                    futureStartup.complete(false);
                    return;
                }

                if (OS.isLinux()) {
                    try {
                        loadCEF();
                    } catch (Throwable e) {
                        CefLog.Error("Can't load libcef, error: %s", e.getMessage());
                        futureStartup.complete(false);
                        return;
                    }
                }

                boolean result = nativeStartup.apply(getPathToFrameworkOSX());
                if (!result)
                    CefLog.Error("N_Startup failed.");
                futureStartup.complete(result);
            } catch (Throwable e) {
                CefLog.Error(e.getMessage());
                futureStartup.completeExceptionally(e);
            }
        };
        new Thread(r, "CefStartup-thread").start();
        return true;
    }

    static private String getPathToFrameworkOSX() {
        if (!OS.isMacintosh())
            return null;

        String pathToCef = ALT_CEF_FRAMEWORK_DIR;
        if (pathToCef == null) {
            String CONTENTS_PATH = System.getProperty("java.home") + "/..";
            pathToCef = CONTENTS_PATH + "/Frameworks/Chromium Embedded Framework.framework";
        }
        return Utils.normalizePath(pathToCef);
    }

    static private String getPathToHelperOSX() {
        if (!OS.isMacintosh())
            return null;

        if (tmpLibDir != null)
            return tmpLibDir + "/jcef Helper.app";

        String pathToHelpers = ALT_CEF_HELPER_APP_DIR;
        if (pathToHelpers == null) {
            String CONTENTS_PATH = System.getProperty("java.home") + "/..";
            pathToHelpers = CONTENTS_PATH + "/Frameworks/jcef Helper.app";
        }

        return Utils.normalizePath(pathToHelpers);
    }

    static String[] fixOSXPathsInArgs(String[] args) {
        if (!OS.isMacintosh())
            return args;

        List<String> fixed = new ArrayList<>();
        if (args != null)
            for (String arg: args) {
                if (!arg.contains("--framework-dir-path=") && !arg.contains("--browser-subprocess-path=") && !arg.contains("--main-bundle-path="))
                    fixed.add(arg);
                else
                    CefLog.Debug("Path-argument '%s' will be replaced.", arg);
            }

        fixed.add("--framework-dir-path=" + getPathToFrameworkOSX());

        String pathToHelpers = getPathToHelperOSX();
        fixed.add("--browser-subprocess-path=" + pathToHelpers + "/Contents/MacOS/jcef Helper");
        fixed.add("--main-bundle-path=" + pathToHelpers);

        String[] result = fixed.toArray(new String[]{});
        if (CefLog.IsDebugEnabled())
            CefLog.Debug("Fixed args: %s", Arrays.toString(result));
        return result;
    }

    static void setPathsInSettings(CefSettings settings) {
        // Avoid to override user values by testing on NULL
        if (OS.isMacintosh()) {
            if (settings.browser_subprocess_path == null) {
                settings.browser_subprocess_path = getPathToHelperOSX() + "/Contents/MacOS/jcef Helper";
                CefLog.Debug("Set default browser_subprocess_path: %s", settings.browser_subprocess_path);
            }
        } else if (OS.isWindows()) {
            String pathToCef = ALT_CEF_FRAMEWORK_DIR;
            String pathToHelpers = ALT_CEF_HELPER_APP_DIR;
            if (pathToCef == null) {
                pathToCef = System.getProperty("java.home") + "/lib";
                pathToHelpers = System.getProperty("java.home") + "/bin";
            }

            if (settings.resources_dir_path == null) {
                settings.resources_dir_path = pathToCef;
                CefLog.Debug("Set default resources_dir_path: %s", settings.resources_dir_path);
            }
            if (settings.locales_dir_path == null) {
                settings.locales_dir_path = pathToCef + "/locales";
                CefLog.Debug("Set default locales_dir_path: %s", settings.locales_dir_path);
            }
            if (settings.browser_subprocess_path == null) {
                if (tmpLibDir != null) {
                    String prev = settings.browser_subprocess_path;
                    settings.browser_subprocess_path = tmpLibDir + "/jcef_helper.exe";
                    CefLog.Debug("Set custom browser_subprocess_path: %s (was %s)", settings.browser_subprocess_path, prev);
                } else {
                    settings.browser_subprocess_path = pathToHelpers + "/jcef_helper.exe";
                    CefLog.Debug("Set default browser_subprocess_path: %s", settings.browser_subprocess_path);
                }
            }

        } else if (OS.isLinux()) {
            String pathToCef = ALT_CEF_FRAMEWORK_DIR;
            String pathToHelpers = ALT_CEF_HELPER_APP_DIR;
            if (pathToCef == null) {
                pathToHelpers = pathToCef = System.getProperty("java.home") + "/lib";
            }

            if (settings.browser_subprocess_path == null) {
                if (tmpLibDir != null) {
                    String prev = settings.browser_subprocess_path;
                    settings.browser_subprocess_path = tmpLibDir + "/jcef_helper";
                    CefLog.Debug("Set custom browser_subprocess_path: %s (was %s)", settings.browser_subprocess_path, prev);
                } else {
                    settings.browser_subprocess_path = pathToHelpers + "/jcef_helper";
                    CefLog.Debug("Set default browser_subprocess_path: %s", settings.browser_subprocess_path);
                }
            }
            if (settings.resources_dir_path == null) {
                settings.resources_dir_path = pathToCef;
                CefLog.Debug("Set default resources_dir_path: %s", settings.resources_dir_path);
            }
            if (settings.locales_dir_path == null) {
                settings.locales_dir_path = pathToCef + "/locales";
                CefLog.Debug("Set default locales_dir_path: %s", settings.locales_dir_path);
            }
        }
    }

    private static void testSleep(int ms) {
        if (ms > 0) {
            CefLog.Debug("testSleep %s ms", ms);
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
