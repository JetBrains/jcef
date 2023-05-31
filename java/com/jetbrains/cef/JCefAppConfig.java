// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.jetbrains.cef;

import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.misc.Utils;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Anton Tarasov
 */
public abstract class JCefAppConfig {
    protected final CefSettings cefSettings = new CefSettings();
    protected final List<String> appArgs = new ArrayList<>();

    private static final AtomicReference<Double> forceDeviceScaleFactor = new AtomicReference<>(Double.valueOf(0));

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

    protected abstract void init();

    private static class JCefAppConfigMac extends JCefAppConfig {
        @Override
        protected void init() {
            appArgs.add("--disable-in-process-stack-traces");
            appArgs.add("--use-mock-keychain");
            appArgs.add("--disable-features=SpareRendererForSitePerProcess");
        }
    }

    private static class JCefAppConfigWindows extends JCefAppConfig {
        @Override
        protected void init() {
            appArgs.add("--disable-features=SpareRendererForSitePerProcess");
        }
    }

    private static class JCefAppConfigLinux extends JCefAppConfig {
        @Override
        protected void init() {
            double scale = getDeviceScaleFactor(null);
            appArgs.add("--force-device-scale-factor=" + scale);
            appArgs.add("--disable-features=SpareRendererForSitePerProcess");
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
            }
            else {
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
                }
                else {
                    forceDeviceScaleFactor.set(Double.valueOf(-1));
                }
            }
        }
        return forceDeviceScaleFactor.get();
    }
}
