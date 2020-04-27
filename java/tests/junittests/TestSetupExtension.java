// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.handler.CefAppHandlerAdapter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    private static CountDownLatch countdown_ = new CountDownLatch(1);

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

        String[] appArgs = null;
        if (OS.isMacintosh()) {
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
            appArgs = new String[] {
                    "--framework-dir-path=" + normalize(ALT_CEF_FRAMEWORK_DIR),
                    "--browser-subprocess-path=" + normalize(ALT_CEF_HELPER_APP_DIR + "/Contents/MacOS/jcef Helper"),
                    "--main-bundle-path=" + normalize(ALT_CEF_HELPER_APP_DIR),
                    "--disable-in-process-stack-traces"
            };
        };

        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = false;
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_ERROR;

        if (OS.isLinux() || OS.isWindows()) {
            String JCEF_PATH = System.getProperty("java.home") + (OS.isLinux() ? "/lib" : "/bin");
            settings.resources_dir_path = JCEF_PATH;
            settings.locales_dir_path = JCEF_PATH + "/locales";
            settings.browser_subprocess_path = JCEF_PATH + "/jcef_helper";
        }

        CefApp.addAppHandler(new CefAppHandlerAdapter(appArgs) {
            @Override
            public void stateHasChanged(org.cef.CefApp.CefAppState state) {
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

        CefApp.getInstance().dispose();

        // Wait for CEF shutdown to complete.
        try {
            if(!countdown_.await(TIMEOUT, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out after " + TIMEOUT + " seconds");
            }
        } catch (InterruptedException e) {
        }
    }
}
