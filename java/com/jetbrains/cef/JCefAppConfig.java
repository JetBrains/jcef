// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.jetbrains.cef;

import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.OS;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Tarasov
 */
public abstract class JCefAppConfig {
    /**
     * Public JCEF API version in form {@code <major>.<minor>};
     * It may not correlate with {@link #getVersion() JCEF version}
     * <ul>
     *     <li>{@link #MINOR Minor version} is incremented when backward compatible API changes are made (API is extended)</li>
     *     <li>{@link #MAJOR Major version} is incremented when non-backward compatible API changes are made</li>
     * </ul>
     * Current API version is {@value #MAJOR}.{@value #MINOR}
     */
    public static final class ApiVersion {
        private static final int MAJOR = 1;
        private static final int MINOR = 1;

        private static final ApiVersion INSTANCE = new ApiVersion();
        private ApiVersion() {}

        public int getMajor() {
            return MAJOR;
        }

        public int getMinor() {
            return MINOR;
        }

        /**
         * Checks if current JCEF API version is compatible with requested (target) API version
         */
        public boolean isCompatible(int targetMajor, int targetMinor) {
            return getMajor() == targetMajor && getMinor() >= targetMinor;
        }

        @Override
        public String toString() {
            return "JCEF API " + getMajor() + "." + getMinor();
        }
    }

    protected final CefSettings cefSettings = new CefSettings();
    protected final List<String> appArgs = new ArrayList<>();

    private static class Holder {
        static JCefAppConfig INSTANCE;

        static {
            if (OS.isMacintosh()) {
                INSTANCE = new JCefAppConfigMac();
            }
            else if (OS.isLinux()) {
                INSTANCE = new JCefAppConfigLinux();
            }
            else if (OS.isWindows()) {
                INSTANCE = new JCefAppConfigWindows();
            }
            else {
                INSTANCE = null;
                assert false : "JCEF: unknown platform";
            }
        }
    }

    public String[] getAppArgs() {
        return appArgs.toArray(new String[0]);
    }

    public List<String> getAppArgsAsList() {
        return appArgs;
    }

    public CefSettings getCefSettings() {
        return cefSettings;
    }

    /**
     * @throws IllegalStateException in case of unsupported platform
     */
    public static JCefAppConfig getInstance() {
        if (Holder.INSTANCE != null) {
            Holder.INSTANCE.init();
        } else {
            throw new IllegalStateException("JCEF is not supported on this platform");
        }
        return Holder.INSTANCE;
    }

    /**
     * Returns the full version string before {@link org.cef.CefApp} is created.
     * Otherwise use {@link CefApp#getVersion()}.
     */
    public static String getVersion() {
        try (InputStream inputStream = JCefAppConfig.class.getResourceAsStream("version.info")) {
            return new BufferedReader(new InputStreamReader(inputStream)).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns runtime {@link ApiVersion JCEF API version}<br>
     * Current API version is {@value ApiVersion#MAJOR}.{@value ApiVersion#MINOR}
     */
    public static ApiVersion getApiVersion() {
        return ApiVersion.INSTANCE;
    }

    protected abstract void init();

    private static class JCefAppConfigMac extends JCefAppConfig {
        @Override
        protected void init() {
            String ALT_CEF_FRAMEWORK_DIR = System.getenv("ALT_CEF_FRAMEWORK_DIR");
            String ALT_CEF_HELPER_APP_DIR = System.getenv("ALT_CEF_HELPER_APP_DIR");
            if (ALT_CEF_FRAMEWORK_DIR == null || ALT_CEF_HELPER_APP_DIR == null) {
                String CONTENTS_PATH = System.getProperty("java.home") + "/..";
                if (ALT_CEF_FRAMEWORK_DIR == null) {
                    ALT_CEF_FRAMEWORK_DIR = CONTENTS_PATH + "/Frameworks/Chromium Embedded Framework.framework";
                }
                if (ALT_CEF_HELPER_APP_DIR == null) {
                    ALT_CEF_HELPER_APP_DIR = CONTENTS_PATH + "/Frameworks/jcef Helper.app";
                }
            }
            appArgs.add("--framework-dir-path=" + normalize(ALT_CEF_FRAMEWORK_DIR));
            appArgs.add("--browser-subprocess-path=" + normalize(ALT_CEF_HELPER_APP_DIR + "/Contents/MacOS/jcef Helper"));
            appArgs.add("--main-bundle-path=" + normalize(ALT_CEF_HELPER_APP_DIR));
            appArgs.add("--disable-in-process-stack-traces");
            appArgs.add("--use-mock-keychain");
            appArgs.add("--disable-features=SpareRendererForSitePerProcess");
        }

        // CEF does not accept ".." in path
        static String normalize(String path) {
            try {
                return new File(path).getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class JCefAppConfigWindows extends JCefAppConfig {
        @Override
        protected void init() {
            String JCEF_PATH = System.getProperty("java.home") + "/bin";
            cefSettings.resources_dir_path = JCEF_PATH;
            cefSettings.locales_dir_path = JCEF_PATH + "/locales";
            cefSettings.browser_subprocess_path = JCEF_PATH + "/jcef_helper";
            appArgs.add("--disable-features=SpareRendererForSitePerProcess");
        }
    }

    private static class JCefAppConfigLinux extends JCefAppConfig {
        @Override
        protected void init() {
            String JCEF_PATH = System.getProperty("java.home") + "/lib";
            cefSettings.resources_dir_path = JCEF_PATH;
            cefSettings.locales_dir_path = JCEF_PATH + "/locales";
            cefSettings.browser_subprocess_path = JCEF_PATH + "/jcef_helper";
            double scale = sysScale();
            System.setProperty("jcef.forceDeviceScaleFactor", Double.toString(scale));
            appArgs.add("--force-device-scale-factor=" + scale);
            appArgs.add("--disable-features=SpareRendererForSitePerProcess");
        }
    }

    private static double sysScale() {
        GraphicsDevice device = null;
        double scale = 1.0;
        try {
            device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        } catch (HeadlessException ignore) {
        }
        if (device != null) {
            GraphicsConfiguration gc = device.getDefaultConfiguration();
            if (gc != null) {
                scale = gc.getDefaultTransform().getScaleX();
            }
        }
        return scale;
    }
}
