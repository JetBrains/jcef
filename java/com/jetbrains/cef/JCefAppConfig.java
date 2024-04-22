// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.jetbrains.cef;

import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.SystemBootstrap;
import org.cef.misc.Utils;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Anton Tarasov
 * TODO: Why it's abstact? Check if removing abstract can be promoted
 */
public abstract class JCefAppConfig {
    protected final CefSettings cefSettings = new CefSettings();
    protected final List<String> appArgs = new ArrayList<>();
    protected SystemBootstrap.Loader loader = null;
    private String cefFrameworkPathOSC = null;
    private static final AtomicReference<Double> forceDeviceScaleFactor = new AtomicReference<>(Double.valueOf(0));

    public String[] getAppArgs() {
        return appArgs.toArray(new String[0]);
    }

    public List<String> getAppArgsAsList() {
        return appArgs;
    }

    public CefSettings getCefSettings() {
        return cefSettings;
    }

    public SystemBootstrap.Loader getLoader() {
        return loader;
    }

    public static JCefAppConfig getInstance(String nativeBundlePath) {
        JCefAppConfig appConfig = new JCefAppConfig() {
        };
        if (OS.isMacintosh()) {
            appConfig.cefFrameworkPathOSC = Utils.pathOf(nativeBundlePath, "Frameworks/Chromium Embedded Framework.framework");
            appConfig.appArgs.add("--framework-dir-path=" + appConfig.cefFrameworkPathOSC);
            appConfig.appArgs.add("--main-bundle-path=" + Utils.pathOf(nativeBundlePath, "Frameworks/jcef Helper.app"));
            appConfig.appArgs.add("--browser-subprocess-path=" + Utils.pathOf(nativeBundlePath, "Frameworks/jcef Helper.app/Contents/MacOS/jcef Helper"));
            appConfig.loader = new SystemBootstrap.Loader() {
                @Override
                public void loadLibrary(String libname) {
                    libname = Utils.pathOf(nativeBundlePath, "lib" + libname + ".dylib");
                    System.load(libname);
                }
            };
        } else if (OS.isLinux()) {
            appConfig.cefSettings.browser_subprocess_path = Utils.pathOf(nativeBundlePath, "jcef_helper");
            appConfig.cefSettings.resources_dir_path = Utils.pathOf(nativeBundlePath);
            appConfig.cefSettings.locales_dir_path = Utils.pathOf(nativeBundlePath, "locales");
            appConfig.loader = new SystemBootstrap.Loader() {
                @Override
                public void loadLibrary(String libname) {
                    libname = Utils.pathOf(nativeBundlePath, "lib" + libname + ".so");
                    System.load(libname);
                }
            };
        } else if (OS.isWindows()) {
            appConfig.cefSettings.browser_subprocess_path = Utils.pathOf(nativeBundlePath, "jcef_helper.exe");
            appConfig.cefSettings.resources_dir_path = Utils.pathOf(nativeBundlePath);
            appConfig.cefSettings.locales_dir_path = Utils.pathOf(nativeBundlePath, "locales");

            appConfig.loader = new SystemBootstrap.Loader() {
                final private Set<String> bundledLibs = new HashSet<>(Arrays.asList("chrome_elf", "jcef", "libcef"));
                @Override
                public void loadLibrary(String libname) {
                    if (bundledLibs.contains(libname)) {
                        System.load(Utils.pathOf(nativeBundlePath, libname + ".dll"));
                    } else {
                        System.loadLibrary(libname);
                    }
                }
            };

        } else {
            throw new IllegalStateException("JCEF is not supported on this platform: " + System.getProperty("os.name", "unknown"));
        }

        return appConfig;
    }

    /**
     * @throws IllegalStateException in case of unsupported platform
     */
    public static JCefAppConfig getInstance() {
        JCefAppConfig appConfig = new JCefAppConfig() {
        };
        if (OS.isMacintosh()) {
            String javaRoot = Utils.pathOf(System.getProperty("java.home"), "/..");
            String frameworkPath = Utils.pathOf(javaRoot, "/Frameworks/Chromium Embedded Framework.framework");
            String cefHelperPath = Utils.pathOf(javaRoot, "/Frameworks/jcef Helper.app");
            String subprocessPath = Utils.pathOf(cefHelperPath, "/Contents/MacOS/jcef Helper");

            appConfig.appArgs.add("--framework-dir-path=" + frameworkPath);
            appConfig.appArgs.add("--browser-subprocess-path=" + subprocessPath);
            appConfig.appArgs.add("--main-bundle-path=" + cefHelperPath);

            appConfig.appArgs.add("--disable-in-process-stack-traces");
            appConfig.appArgs.add("--use-mock-keychain");
            appConfig.appArgs.add("--disable-features=SpareRendererForSitePerProcess");
        } else if (OS.isLinux()) {
            String libPath = Utils.pathOf(System.getProperty("java.home"), "lib");
            appConfig.cefSettings.resources_dir_path = libPath;
            appConfig.cefSettings.locales_dir_path = Utils.pathOf(libPath, "locales");
            appConfig.cefSettings.browser_subprocess_path = Utils.pathOf(libPath, "jcef_helper");

            double scale = getDeviceScaleFactor(null);
            appConfig.appArgs.add("--force-device-scale-factor=" + scale);
            appConfig.appArgs.add("--disable-features=SpareRendererForSitePerProcess");
        } else if (OS.isWindows()) {
            String binPath = System.getProperty("java.home") + "/bin";
            String libPath = System.getProperty("java.home") + "/lib";
            appConfig.cefSettings.resources_dir_path = libPath;
            appConfig.cefSettings.locales_dir_path = libPath + "/locales";
            appConfig.cefSettings.browser_subprocess_path = binPath + "/jcef_helper.exe";

            appConfig.appArgs.add("--disable-features=SpareRendererForSitePerProcess");
        } else {
            throw new IllegalStateException("JCEF is not supported on this platform: " + System.getProperty("os.name", "unknown"));
        }

        appConfig.loader = new SystemBootstrap.Loader() {
            @Override
            public void loadLibrary(String libname) {
                System.loadLibrary(libname);
            }
        };

        return appConfig;
    }

    /**
     * @deprecated don't use. This function is temporary here.
     */
    @Deprecated
    public static String getJbrFrameworkPathOSX() {
        if (OS.isMacintosh()) {
            return Utils.pathOf(System.getProperty("java.home"), "../Frameworks/Chromium Embedded Framework.framework");
        }
        return null;
    }

    /**
     * Tries to load full JCEF version string from version.info file
     */
    private static String getVersionEx() throws IOException {
        try (InputStream inputStream = JCefAppConfig.class.getResourceAsStream("version.info")) {
            return new BufferedReader(new InputStreamReader(inputStream)).readLine();
        }
    }

    /**
     * Returns the full version string before {@link org.cef.CefApp} is created.
     * Otherwise use {@link CefApp#getVersion()}.
     */
    public static String getVersion() {
        try {
            return getVersionEx();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns runtime {@link JCefVersionDetails JCEF version details}
     */
    public static JCefVersionDetails getVersionDetails() throws JCefVersionDetails.VersionUnavailableException {
        try {
            return new JCefVersionDetails(getVersionEx());
        } catch (IOException e) {
            throw new JCefVersionDetails.VersionUnavailableException("Unable to load version information", e);
        }
    }


    public static double getDeviceScaleFactor(/*@Nullable*/Component component) {
        if (GraphicsEnvironment.isHeadless()) {
            return 1.0;
        }
        double scale = getForceDeviceScaleFactor();
        if (scale > 0) return scale;

        GraphicsDevice device = null;
        try {
            if (component != null && component.getGraphicsConfiguration() != null) {
                device = component.getGraphicsConfiguration().getDevice();
            } else {
                device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        if (device != null) {
            GraphicsConfiguration gc = device.getDefaultConfiguration();
            if (gc != null) {
                scale = gc.getDefaultTransform().getScaleX();
            }
        }
        return scale;
    }

    /**
     * Defined to support IDE-managed HiDPI mode in IDEA, undefined in JRE-managed HiDPI.
     */
    @Deprecated
    public static double getForceDeviceScaleFactor() {
        if (forceDeviceScaleFactor.get() == 0) {
            synchronized (forceDeviceScaleFactor) {
                String prop = System.getProperty("jcef.forceDeviceScaleFactor");
                if (prop != null) {
                    try {
                        forceDeviceScaleFactor.set(Double.parseDouble(prop));
                    } catch (NumberFormatException e) {
                        forceDeviceScaleFactor.set(Double.valueOf(-1));
                        e.printStackTrace();
                    }
                } else {
                    forceDeviceScaleFactor.set(Double.valueOf(-1));
                }
            }
        }
        return forceDeviceScaleFactor.get();
    }
}
