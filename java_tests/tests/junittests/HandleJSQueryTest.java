package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest.TransitionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @summary
 * 1. testDoubleRequest is regression test for JBR-2430. The test checks that JS Query is handled in 2nd opened browser.
 * 2. Other tests are useful to debug JBR-4475.
 */
@ExtendWith(TestSetupExtension.class)
public class HandleJSQueryTest {
    @Test
    public void testJsRequest10Times() throws InterruptedException {
        CefLog.Info("Start testJsRequest10Times");
        final long count = 10;
        for (long c = 0; c < count; ++c) {
            final CountDownLatch l = new CountDownLatch(1);
            CefBrowserFrame browserFrame = new CefBrowserFrame(l);

            try {
                SwingUtilities.invokeLater(()->browserFrame.initUI());
                l.await(10, TimeUnit.SECONDS);

                if (browserFrame.callbackCounter == 0) {
                    CefLog.Error("JS Query %d was not handled in browser: %d [%s]", c, browserFrame.browserNumber, browserFrame.browser.getCefBrowser());
                    throw new RuntimeException(time() + "test FAILED. JS Query was not handled in opened browser: " + browserFrame.browserNumber);
                }
            } finally {
                browserFrame.closeWindow();
            }
        }
        CefLog.Info("test PASSED");
    }

    @Test
    public void testJsRequestInOneBrowser10Times() throws InterruptedException {
        CefLog.Info("Start testJsRequestInOneBrowser10Times");
        final int count = 10;
        final CountDownLatch l = new CountDownLatch(count);
        CefBrowserFrame browserFrame = new CefBrowserFrame(l);
        try {
            SwingUtilities.invokeLater(()->browserFrame.initUI());
            l.await(count*5, TimeUnit.SECONDS);

            if (browserFrame.callbackCounter != count) {
                CefLog.Error("Some JS Query was not handled in browser %s, callbackCounter=%d", browserFrame.browser.getCefBrowser(), browserFrame.callbackCounter);
                throw new RuntimeException(time() + "test FAILED. JS Query was not handled: callbackCounter=" + browserFrame.callbackCounter);
            }
        } finally {
            browserFrame.closeWindow();
        }
        CefLog.Info("test PASSED");
    }

    private static String time() {
        return new SimpleDateFormat("MM.ss.SSS: ").format(new Date());
    }

    @Test
    public void testDoubleRequest() throws InterruptedException {
        CefLog.Info("Start testDoubleRequest");
        final int count = 1;
        final CountDownLatch firstLatch = new CountDownLatch(count);
        final CountDownLatch secondLatch = new CountDownLatch(count);
        CefBrowserFrame firstBrowser = new CefBrowserFrame(firstLatch);
        CefBrowserFrame secondBrowser = new CefBrowserFrame(secondLatch);

        try {
            SwingUtilities.invokeLater(()->firstBrowser.initUI());
            firstLatch.await(10, TimeUnit.SECONDS);
            if (firstBrowser.callbackCounter == 0) {
                CefLog.Error("JS Query was not handled in 1 browser %s", firstBrowser.browser.getCefBrowser());
                throw new RuntimeException("test FAILED. JS Query was not handled in 1 opened browser");
            }

            SwingUtilities.invokeLater(()->secondBrowser.initUI());
            secondLatch.await(10, TimeUnit.SECONDS);
            if (secondBrowser.callbackCounter == 0) {
                CefLog.Error("JS Query was not handled in 2 browser %s", secondBrowser.browser.getCefBrowser());
                throw new RuntimeException("test FAILED. JS Query was not handled in 2 opened browser");
            }
        } finally {
            firstBrowser.closeWindow();
            secondBrowser.closeWindow();
        }
        CefLog.Info("test PASSED");
    }

    static class CefBrowserFrame extends JFrame {
        static volatile int ourBrowserNumber;

        private final JBCefBrowser browser;
        private final List<JSRequest> requests = new ArrayList<>();

        private final CountDownLatch latch;
        private int browserNumber;
        private int callbackCounter;
        boolean isJSRequestsPosted = false;

        public CefBrowserFrame(final CountDownLatch latch) {
            super("JCEF HandleJSQueryTest");
            this.browserNumber = ourBrowserNumber++;
            this.latch = latch;

            browser = new JBCefBrowser(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadStart(CefBrowser browser, CefFrame frame, TransitionType transitionType) {
                    CefLog.Info("onLoadStart: browser " + browserNumber);
                }

                @Override
                public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                    CefLog.Info("onLoadEnd: browser " + browserNumber);
                    postJSRequests();
                }

                @Override
                public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                    CefLog.Info("onLoadingStateChange: browser " + browserNumber);
                    super.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
                }
            });

            final long requestCount = latch.getCount();
            createRequests(requestCount);
        }

        public void initUI() {
            getContentPane().add(browser.getComponent());
            setSize(640, 480);
            setVisible(true);

            reload();
        }

        public void closeWindow() {
            for (JSRequest r: requests)
                browser.getCefClient().removeMessageRouter(r.msgRouter);
            browser.dispose();
            browser.awaitClientDisposed();
            super.dispose();
        }

        public void reload() {
            browser.loadURL("about:blank");
        }

        private void createRequests(long requestCount) {
            // create JS-requests
            CefLog.Info("create JS of browser " + browserNumber + ", count " + requestCount);
            for (long c = 0; c < requestCount; ++c) {
                JSRequest r = new JSRequest("" + browserNumber + "_" + c);
                requests.add(r);
                browser.getCefClient().addMessageRouter(r.msgRouter);
            }
        }

        private void postJSRequests() {
            if (isJSRequestsPosted)
                return;

            CefLog.Info("post " + requests.size() + " JS requests of browser " + browserNumber);
            for (JSRequest r: requests) {
                browser.getCefBrowser().executeJavaScript(r.jsQuery, "", 0);
            }
            isJSRequestsPosted = true;
        }

        public JBCefBrowser getBrowser() {
            return browser;
        }

        class JSRequest {
            final CefMessageRouter.CefMessageRouterConfig config;
            final CefMessageRouter msgRouter;
            final String jsQuery;

            public JSRequest(String uid) {
                config = new org.cef.browser.CefMessageRouter.CefMessageRouterConfig();
                config.jsQueryFunction = "cef_query_" + uid;
                config.jsCancelFunction = "cef_query_cancel_" + uid;
                msgRouter = CefMessageRouter.create(config);

                msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
                    @Override
                    public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
                                           boolean persistent, CefQueryCallback callback) {
                        CefLog.Info("the query with request " + request + " is handled.");
                        callbackCounter++;
                        latch.countDown();
                        return true;
                    }
                }, true);

                jsQuery = "window." + config.jsQueryFunction + "({request: '" + config.jsQueryFunction + "'});";
            }
        }
    }
}