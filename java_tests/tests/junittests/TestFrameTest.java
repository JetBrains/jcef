// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.misc.CefLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

// Test the TestFrame implementation.
@ExtendWith(TestSetupExtension.class)
class TestFrameTest {
    private boolean gotSetupTest_ = false;
    private boolean gotCleanupTest_ = false;
    private boolean gotLoadingStateChange_ = false;

    @Test
    void minimal() {
        final String testUrl = "http://test.com/test.html";
        TestFrame frame = new TestFrame() {
            @Override
            protected void setupTest() {
                assertFalse(gotSetupTest_);
                gotSetupTest_ = true;

                addResource(testUrl, "<html><body>Test!</body></html>", "text/html");

                createBrowser(testUrl);

                super.setupTest();
            }

            @Override
            protected void cleanupTest() {
                assertFalse(gotCleanupTest_);
                gotCleanupTest_ = true;

                super.cleanupTest();
            }

            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading,
                    boolean canGoBack, boolean canGoForward) {
                if (!isLoading) {
                    assertFalse(gotLoadingStateChange_);
                    gotLoadingStateChange_ = true;
                    terminateTest();
                }
            }
        };

        frame.awaitCompletion();

        assertTrue(gotSetupTest_);
        assertTrue(gotLoadingStateChange_);
        assertTrue(gotCleanupTest_);
    }
}
