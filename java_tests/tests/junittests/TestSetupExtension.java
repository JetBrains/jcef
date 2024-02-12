// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import tests.OsrSupport;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

// All test cases must install this extension for CEF to be properly initialized
// and shut down.
//
// For example:
//
//   @ExtendWith(TestSetupExtension.class)
//   class FooTest {
//        @Test
//        void testCaseThatRequiresCEF() {}
//   }
//
// This code is based on https://stackoverflow.com/a/51556718.
public class TestSetupExtension
        implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private static final int TIMEOUT = 5;
    private static boolean initialized_ = false;
    private static CountDownLatch stateTerminated_ = new CountDownLatch(1);

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!initialized_) {
            initialized_ = true;
            initialize(context);
        }
    }

    public static void enableVerboseLogging() {
        System.setProperty("jcef.tests.verbose", "true");
        System.setProperty("jcef.trace.cefbrowser_n.lifespan", "true");
        System.setProperty("jcef.trace.cefclient.lifespan", "true");
        System.setProperty("jcef.trace.cefapp.lifespan", "true");
        System.setProperty("jcef.trace.cefbrowserwr.addnotify", "true");
        System.setProperty("jcef.log.trace_thread", "true");
    }

    // Executed before any tests are run.
    private void initialize(ExtensionContext context) {
        TestSetupContext.initialize(context);

        if (TestSetupContext.debugPrint()) {
            CefLog.Info("TestSetupExtension.initialize");
        }

        // Register a callback hook for when the root test context is shut down.
        context.getRoot().getStore(GLOBAL).put("jcef_test_setup", this);

        initializeCef();
    }

    public static void initializeCef() {
        CefLog.init(Utils.getString("JCEF_TESTS_LOG_FILE"), CefSettings.LogSeverity.LOGSEVERITY_VERBOSE);

        // Enable debug logging for junit tests by default
        enableVerboseLogging();

        // Perform startup initialization on platforms that require it.
        CefApp.startup(new String[]{});

        JCefAppConfig config = JCefAppConfig.getInstance();
        String[] appArgs = config.getAppArgs();
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(appArgs));

        if (OS.isLinux())
            args.add("--password-store=basic");

        String extraArgsProp = Utils.getString("JCEF_TESTS_EXTRA_ARGS", "");
        if (!extraArgsProp.isEmpty()) {
            String[] extraArgs = extraArgsProp.split(";");
            if (extraArgs.length > 0) {
                CefLog.Info("Use extra CEF args: [" + Arrays.toString(extraArgs) + "]");
                args.addAll(Arrays.asList(extraArgs));
            }
        }

        CefSettings settings = config.getCefSettings();
        settings.windowless_rendering_enabled = OsrSupport.isEnabled();
        String debugPort = Utils.getString("JCEF_DEVTOOL_DEBUG_PORT");
        if (debugPort != null) {
            settings.remote_debugging_port = Integer.parseInt(debugPort);
            args.add("--remote-allow-origins=*");
        }
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        settings.log_file = Utils.getString("JCEF_TESTS_LOG_FILE");
        boolean envSandboxed = Utils.getBoolean("JCEF_TESTS_SANDBOX_ENABLED");
        settings.no_sandbox = !envSandboxed;

        String argsArr[] = args.toArray(new String[0]);
        CefApp.addAppHandler(new CefAppHandlerAdapter(argsArr) {
            @Override
            public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                if (state == CefAppState.TERMINATED) {
                    // Signal completion of CEF shutdown.
                    stateTerminated_.countDown();
                }
            }
            @Override
            public void onBeforeChildProcessLaunch(String command_line) {
                CefLog.Info("Child process launched: " + command_line);
                super.onBeforeChildProcessLaunch(command_line);
            }
        });

        CefLog.Info("settings: %s", settings.getDescription());
        CefLog.Info("args: %s", Arrays.toString(argsArr));

        // Initialize the singleton CefApp instance.
        CefApp.getInstance(settings);
    }

    // Executed after all tests have completed.
    @Override
    public void close() {
        if (TestSetupContext.debugPrint()) {
            CefLog.Info("TestSetupExtension.close");
        }

        if (!Boolean.getBoolean("jcef.junittests.isGradleLauncher")) { // due to https://issues.gradle.org/browse/GRADLE-1903
            CefApp.getInstance().dispose();

            // Wait for CEF shutdown to complete.
            try {
                if (!stateTerminated_.await(TIMEOUT, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timed out after " + TIMEOUT + " seconds");
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
