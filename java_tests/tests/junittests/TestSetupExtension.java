// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import org.cef.misc.CefLog;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import tests.CefInitHelper;

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
    private static boolean initialized_ = false;

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
            CefLog.Info("TestSetupExtension.initialize");
        }

        // Register a callback hook for when the root test context is shut down.
        context.getRoot().getStore(GLOBAL).put("jcef_test_setup", this);

        CefInitHelper.initializeCef();
    }



    // Executed after all tests have completed.
    @Override
    public void close() {
        if (TestSetupContext.debugPrint()) {
            CefLog.Info("TestSetupExtension.close");
        }

        CefInitHelper.shutdonwCef();
    }
}
