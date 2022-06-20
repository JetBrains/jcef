package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.network.CefRequest.TransitionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @summary Regression test for JBR-2430. The test checks that JS Query is handled in 2nd opened browser.
 */
@ExtendWith(TestSetupExtension.class)
public class HandleJSQueryTest {
    @Test
    public void testDoubleRequest() throws InvocationTargetException, InterruptedException {
        final CountDownLatch firstLatch = new CountDownLatch(1);
        final CountDownLatch secondLatch = new CountDownLatch(1);
        CefBrowserFrame firstBrowser = new CefBrowserFrame(firstLatch);
        CefBrowserFrame secondBrowser = new CefBrowserFrame(secondLatch);

        try {
            SwingUtilities.invokeLater(firstBrowser::initUI);
            firstLatch.await(10, TimeUnit.SECONDS);

            SwingUtilities.invokeLater(secondBrowser::initUI);
            secondLatch.await(10, TimeUnit.SECONDS);

            if (CefBrowserFrame.ourCallbackCounter < 2) {
                throw new RuntimeException("Test FAILED. JS Query was not handled in 2nd opened browser");
            }
            System.out.println("Test PASSED");
        } finally {
            System.out.println("Close all windows");
            SwingUtilities.invokeLater(() -> firstBrowser.dispatchEvent(new WindowEvent(firstBrowser, WindowEvent.WINDOW_CLOSING)));
            SwingUtilities.invokeLater(() -> secondBrowser.dispatchEvent(new WindowEvent(secondBrowser, WindowEvent.WINDOW_CLOSING)));
        }
    }

    static class CefBrowserFrame extends JFrame {

        static volatile int ourCallbackCounter;
        static volatile int ourBrowserNumber;

        private final JBCefBrowser browser = new JBCefBrowser();

        private final CountDownLatch latch;
        private int browserNumber;

        public CefBrowserFrame(final CountDownLatch latch) {
            this.latch = latch;
        }

        public void initUI() {
            browserNumber = ourBrowserNumber++;
            CefMessageRouter.CefMessageRouterConfig config = new org.cef.browser.CefMessageRouter.CefMessageRouterConfig();
            config.jsQueryFunction = "cef_query_" + browserNumber;
            config.jsCancelFunction = "cef_query_cancel_" + browserNumber;
            CefMessageRouter msgRouter = CefMessageRouter.create(config);

            msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
                @Override
                public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
                                       boolean persistent, CefQueryCallback callback) {
                    System.out.println("The query with request " + request + " is handled.");
                    ourCallbackCounter++;
                    latch.countDown();
                    return true;
                }
            }, true);

            browser.getCefClient().addMessageRouter(msgRouter);

            browser.getCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadStart(CefBrowser browser, CefFrame frame, TransitionType transitionType) {
                    System.out.println("onLoadStart: Browser " + browserNumber);
                }

                @Override
                public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                    System.out.println("onLoadEnd: Browser " + browserNumber);
                    String jsFunc = "cef_query_" + browserNumber;
                    String jsQuery = "window." + jsFunc + "({request: '" + jsFunc + "'});";
                    browser.executeJavaScript(jsQuery, "", 0);
                }
            });

            getContentPane().add(browser.getCefBrowser().getUIComponent());
            setSize(640, 480);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    browser.dispose();
                    System.out.println("disposed browser " + browserNumber);
                }
            });
            setVisible(true);
        }

        public JBCefBrowser getBrowser() {
            return browser;
        }
    }
}