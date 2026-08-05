// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// Test the DisplayHandler implementation.
@ExtendWith(TestSetupExtension.class)
class DisplayHandlerTest {
    private final String testUrl_ = "https://test.com/test.html";
    private final String testContent_ =
            "<html><head><title>Test Title</title></head><body>Test!</body></html>";

    private boolean gotCallback_ = false;

    @Test
    void onTitleChange() {
        TestFrame frame = new TestFrame() {
            @Override
            protected void setupTest() {
                client_.addDisplayHandler(new CefDisplayHandlerAdapter() {
                    @Override
                    public void onTitleChange(CefBrowser browser, String title) {
                        // Ignore the 2nd OnTitleChange call which arrives after navigation completion.
                        // See: cef/tests/ceftests/display_unittest.cc > TitleTestHandler::OnTitleChange
                        if (gotCallback_) return;
                        gotCallback_ = true;
                        assertEquals("Test Title", title);
                        terminateTest();
                    }
                });

                addResource(testUrl_, testContent_, "text/html");

                createBrowser(testUrl_);

                super.setupTest();
            }
        };

        frame.awaitCompletion();

        assertTrue(gotCallback_);
    }

    @Test
    void onAddressChange() {
        TestFrame frame = new TestFrame() {
            @Override
            protected void setupTest() {
                client_.addDisplayHandler(new CefDisplayHandlerAdapter() {
                    @Override
                    public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                        assertFalse(gotCallback_);
                        gotCallback_ = true;
                        assertEquals(url, testUrl_);
                        terminateTest();
                    }
                });

                addResource(testUrl_, testContent_, "text/html");

                createBrowser(testUrl_);

                super.setupTest();
            }
        };

        frame.awaitCompletion();

        assertTrue(gotCallback_);
    }

    @Test
    void onFullscreenModeChange() {
        TestFrame frame = new TestFrame() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                // The Fullscreen JS API (Element.requestFullscreen) can only be initiated by a user
                // gesture. executeJavaScript() can't request fullscreen mode
                browser.getDevToolsClient().executeDevToolsMethod("Runtime.evaluate",
                        "{\"expression\": \"document.documentElement.requestFullscreen();\", \"userGesture\": true}");
                super.onLoadEnd(browser, frame, httpStatusCode);
            }

            @Override
            protected void setupTest() {
                client_.addDisplayHandler(new CefDisplayHandlerAdapter() {
                    @Override
                    public void onFullscreenModeChange(CefBrowser browser, boolean fullscreen) {
                        if (!fullscreen || gotCallback_) return;
                        gotCallback_ = true;
                        terminateTest();
                    }
                });

                addResource(testUrl_, testContent_, "text/html");

                createBrowser(testUrl_);

                super.setupTest();
            }
        };

        frame.awaitCompletion();

        assertTrue(gotCallback_);
    }
}
