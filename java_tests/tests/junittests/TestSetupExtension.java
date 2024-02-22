// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import tests.OsrSupport;

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
    private static final boolean WAIT_FOR_CEFAPP_INIT = Utils.getBoolean("WAIT_FOR_CEFAPP_INIT", true);
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

        if (Utils.getBoolean("JCEF_TESTS_DISABLE_GPU")) {
            CefLog.Info("Disable GPU in tests.");
            args.add("--disable-gpu");
            args.add("--disable-gpu-compositing");
            args.add("--disable-gpu-vsync");
            args.add("--disable-software-rasterizer");
            args.add("--disable-extensions");
        }

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
        String logLevel = Utils.getString("JCEF_TESTS_LOG_LEVEL", "verbose").toLowerCase();
        settings.log_severity = switch (logLevel) {
            case "disable" -> CefSettings.LogSeverity.LOGSEVERITY_DISABLE;
            case "verbose" -> CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
            case "info" -> CefSettings.LogSeverity.LOGSEVERITY_INFO;
            case "warning" -> CefSettings.LogSeverity.LOGSEVERITY_WARNING;
            case "error" -> CefSettings.LogSeverity.LOGSEVERITY_ERROR;
            case "fatal" -> CefSettings.LogSeverity.LOGSEVERITY_FATAL;
            default -> CefSettings.LogSeverity.LOGSEVERITY_DEFAULT;
        };
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
        CefApp app = CefApp.getInstance(settings);

        if (WAIT_FOR_CEFAPP_INIT) {
            // Wait for initialization
            CountDownLatch latch = new CountDownLatch(1);
            app.onInitialization(s -> latch.countDown());
            final int timeout = 10;
            try {
                latch.await(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                CefLog.Error("InterruptedException: %s", e.getMessage());
            }
            if (latch.getCount() > 0) {
                CefLog.Error("CefApp wasn't initialized in %d seconds. Terminate tests.", timeout);
                throw new RuntimeException("CefApp wasn't initialized in " + timeout + " seconds.");
            }
        }
    }

    // Executed after all tests have completed.
    @Override
    public void close() {
        if (TestSetupContext.debugPrint()) {
            CefLog.Info("TestSetupExtension.close");
        }

        shutdonwCef();
    }

    public static void shutdonwCef() {
        CefLog.Info("Start shutting down CEF.");
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
