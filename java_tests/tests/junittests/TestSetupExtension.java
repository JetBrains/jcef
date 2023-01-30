// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import tests.OsrSupport;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
    private static final boolean SKIP_BASIC_CHECK = Boolean.getBoolean("jcef.tests.skip_basic_check");
    private static final boolean BASIC_CHECK_WITHOUT_UI = Boolean.getBoolean("jcef.tests.basic_check_without_ui");
    private static final int TIMEOUT = 5;
    private static boolean initialized_ = false;
    private static CountDownLatch stateInitialized_ = new CountDownLatch(1);
    private static CountDownLatch stateTerminated_ = new CountDownLatch(1);
    private static CountDownLatch onContextInitialized_ = new CountDownLatch(1);

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
        // Enable debug logging for junit tests by default
        enableVerboseLogging();

        TestSetupContext.initialize(context);

        if (TestSetupContext.debugPrint()) {
            CefLog.Info("TestSetupExtension.initialize");
        }

        // Register a callback hook for when the root test context is shut down.
        context.getRoot().getStore(GLOBAL).put("jcef_test_setup", this);

        // Perform startup initialization on platforms that require it.
        if (!CefApp.startup(new String[]{})) {
            CefLog.Error("Startup initialization failed!");
            return;
        }

        JCefAppConfig config = JCefAppConfig.getInstance();
        String[] appArgs = config.getAppArgs();
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(appArgs));

        String extraArgsProp = System.getProperty("jcef.tests.extra_args", "");
        if (!extraArgsProp.isEmpty()) {
            String[] extraArgs = extraArgsProp.split(",");
            if (extraArgs.length > 0) {
                CefLog.Info("Use extra CEF args: [" + Arrays.toString(extraArgs) + "]");
                args.addAll(Arrays.asList(extraArgs));
            }
        }

        CefSettings settings = config.getCefSettings();
        settings.windowless_rendering_enabled = OsrSupport.isEnabled();
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        settings.log_file = System.getenv("JCEF_TESTS_LOG_FILE");
        String envSandboxed = System.getenv("JCEF_TESTS_SANDBOX_ENABLED");
        settings.no_sandbox = envSandboxed == null || !envSandboxed.trim().equalsIgnoreCase("true");


        String argsArr[] = args.toArray(new String[0]);
        CefApp.addAppHandler(new CefAppHandlerAdapter(argsArr) {
            @Override
            public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                if (state == CefAppState.INITIALIZED) {
                    stateInitialized_.countDown();
                }
                if (state == CefAppState.TERMINATED) {
                    // Signal completion of CEF shutdown.
                    stateTerminated_.countDown();
                }
            }
            @Override
            public void onContextInitialized() {
                onContextInitialized_.countDown();
            }

            @Override
            public void onBeforeChildProcessLaunch(String command_line) {
                CefLog.Info("Child process launched: " + command_line);
                super.onBeforeChildProcessLaunch(command_line);
            }
        });

        CefLog.init(settings); // preinit CefLog for tests (otherwise it can be initialized in EDT later that first test output)
        CefLog.Info("settings: %s", settings.getDescription());
        CefLog.Info("args: %s", Arrays.toString(argsArr));

        // Initialize the singleton CefApp instance.
        CefApp.getInstance(settings);

        if (!SKIP_BASIC_CHECK)
            performBasicJcefTesting();
    }

    private static void _wait(CountDownLatch latch, int timeoutSec, String errorDesc) {
        try {
            if (!latch.await(timeoutSec, TimeUnit.SECONDS)) {
                CefLog.Error(errorDesc);
                throw new RuntimeException(errorDesc);
            }
        } catch (InterruptedException e) {
            CefLog.Error(e.getMessage());
        }
    }

    private static void performBasicJcefTesting() {
        CefLog.Info("Sequentially test basic JCEF functionality");
        final long time0 = System.currentTimeMillis();

        //
        // 1. Create client (CefApp.initialize will be invoked inside). Then setup client for testing (with use of
        //    latches in basic handlers)
        //
        CefClient client = CefApp.getInstance().createClient();
        final long time1 = System.currentTimeMillis();
        CefLog.Info("CefApp initialization spent %d ms", time1 - time0);
        CefLog.Info("Created test client, cinfo: " + client.getInfo());

        // Check correct disposing
        CountDownLatch clientDispose_ = new CountDownLatch(1);
        client.setOnDisposeCallback(()->clientDispose_.countDown());

        // Check CefLifeSpanHandler
        long[] onAfterCreatedTime = new long[]{-1};
        CountDownLatch onAfterCreated_ = new CountDownLatch(1);
        CountDownLatch onBeforeClose_ = new CountDownLatch(1);
        client.addLifeSpanHandler(new LoggingLifeSpanHandler(CefSettings.LogSeverity.LOGSEVERITY_INFO) {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                super.onAfterCreated(browser);
                onAfterCreatedTime[0] = System.currentTimeMillis();
                onAfterCreated_.countDown();
            }
            @Override
            public void onBeforeClose(CefBrowser browser) {
                super.onBeforeClose(browser);
                onBeforeClose_.countDown();
            }
        });

        // Check CefLoadHandler
        CountDownLatch onLoadStart_ = new CountDownLatch(1);
        CountDownLatch onLoadEnd_ = new CountDownLatch(1);
        CountDownLatch onLoadErr_ = new CountDownLatch(1);
        client.addLoadHandler(new LoggingLoadHandler(CefSettings.LogSeverity.LOGSEVERITY_INFO) {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame cefFrame, CefRequest.TransitionType transitionType) {
                super.onLoadStart(browser, cefFrame, transitionType);
                onLoadStart_.countDown();
            }
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame cefFrame, int i) {
                super.onLoadEnd(browser, cefFrame, i);
                onLoadEnd_.countDown();
            }
            @Override
            public void onLoadError(CefBrowser browser, CefFrame cefFrame, ErrorCode errorCode, String errorText, String failedUrl) {
                super.onLoadError(browser, cefFrame, errorCode, errorText, failedUrl);
                onLoadErr_.countDown();
            }
        });

        //
        // 2. Create browser.
        //
        CefBrowser browser;
        if (OsrSupport.isEnabled()) {
            browser = OsrSupport.createBrowser(client, "about:blank");
        } else {
            browser = client.createBrowser("about:blank", false, false);
        }
        CefLog.Info("Created test browser with bid=" + browser.getIdentifier());

        //
        // 3. init UI
        //
        JFrame[] frame = new JFrame[1];
        if (!BASIC_CHECK_WITHOUT_UI && !GraphicsEnvironment.isHeadless()) {
            EventQueue.invokeLater(() -> {
                CefLog.Info("Start test UI initialization");
                frame[0] = new JFrame("JCEF basic test");
                frame[0].add(browser.getUIComponent());
                frame[0].setSize(640, 480);
                frame[0].setLocationRelativeTo(null);
                frame[0].setVisible(true);
                CefLog.Info("Test UI initialized");
            });
        }

        //
        // 4. Perform checks: onAfterCreated -> onLoadStart,onLoadEnd -> CefLifeSpanHandler.onBeforeClosed -> clientDispose_
        //
        _wait(onAfterCreated_, 5, "Native CefBrowser wasn't created");
        CefLog.Info("Native browser creation spent %d ms", onAfterCreatedTime[0] - time1);
        try {
            _wait(onLoadStart_, 5, "onLoadStart wasn't called, bid="+browser.getIdentifier());
        } catch (RuntimeException e) {
            if (onLoadErr_.getCount() <= 0) {
                // empiric observation: onLoadStart can be skipped when onLoadError occured.
                // see https://youtrack.jetbrains.com/issue/JBR-5192/Improve-JCEF-junit-tests#focus=Comments-27-6799179.0-0
                CefLog.Info("onLoadStart wasn't called and onLoadError was observed");
            } else throw e;
        }
        _wait(onLoadEnd_, 10, "onLoadEnd wasn't called");

        // dispose browser and client
        browser.setCloseAllowed(); // Cause browser.doClose() to return false so that OSR browser can close.
        browser.close(true);
        _wait(onBeforeClose_, 5, "onBeforeClose wasn't called");
        client.dispose();
        _wait(clientDispose_, 5, "CefClient wasn't completely disposed: " + client.getInfo());

        if (frame[0] != null)
            frame[0].dispose();

        CefLog.Info("Basic checks spent %d ms", System.currentTimeMillis() - time0);
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
