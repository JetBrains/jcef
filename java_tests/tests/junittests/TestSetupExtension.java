// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefSettings;
import org.cef.handler.CefAppHandlerAdapter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

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
    private static final boolean DISABLE_GPU = Boolean.getBoolean("jcef.tests.disable_gpu");
    private static final int TIMEOUT = 5;
    private static boolean initialized_ = false;
    private static CountDownLatch countdown_ = new CountDownLatch(1);

    private static Function<CefAppState, Void> ourCefAppStateHandler;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!initialized_) {
            initialized_ = true;
            initialize(context);
        }
    }

    // Executed before any tests are run.
    private void initialize(ExtensionContext context) {
        TestSetupContext.initialize(context);

        if (TestSetupContext.debugPrint()) {
            System.out.println("TestSetupExtension.initialize");
        }

        // Register a callback hook for when the root test context is shut down.
        context.getRoot().getStore(GLOBAL).put("jcef_test_setup", this);

        // Perform startup initialization on platforms that require it.
        if (!CefApp.startup(new String[]{})) {
            System.out.println("Startup initialization failed!");
            return;
        }

        JCefAppConfig config = JCefAppConfig.getInstance();
        String[] appArgs = config.getAppArgs();
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(appArgs));
        if (DISABLE_GPU) {
            args.add("--disable-gpu");
            args.add("--disable-gpu-compositing");
        }

        CefSettings settings = config.getCefSettings();
        settings.windowless_rendering_enabled = false;
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_ERROR;
        settings.no_sandbox = true;

        CefApp.addAppHandler(new CefAppHandlerAdapter(args.toArray(new String[0])) {
            @Override
            public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                final Function<CefAppState, Void> f = ourCefAppStateHandler;
                if (f != null) f.apply(state);

                if (state == CefAppState.TERMINATED) {
                    // Signal completion of CEF shutdown.
                    countdown_.countDown();
                }
            }
        });

        // Initialize the singleton CefApp instance.
        CefApp.getInstance(settings);
    }

    private static String normalize(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Executed after all tests have completed.
    @Override
    public void close() {
        if (TestSetupContext.debugPrint()) {
            System.out.println("TestSetupExtension.close");
        }

        if (!Boolean.getBoolean("jcef.junittests.isGradleLauncher")) { // due to https://issues.gradle.org/browse/GRADLE-1903
            CefApp.getInstance().dispose();

            // Wait for CEF shutdown to complete.
            try {
                if (!countdown_.await(TIMEOUT, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timed out after " + TIMEOUT + " seconds");
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public static void setCefAppStateHandler(Function<CefApp.CefAppState, Void> stateHandler) {
        ourCefAppStateHandler = stateHandler;
    }

}
