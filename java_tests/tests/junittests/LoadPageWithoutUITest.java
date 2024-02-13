package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tests.OsrSupport;

import javax.swing.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @requires (os.arch == "amd64" | os.arch == "x86_64" | (os.arch == "aarch64" & os.family == "mac"))
 * @summary Regression test for JBR-2259. The test checks that website is loaded with and without showing Browser UI.
 */


/**
 * Description:
 * The test detects callbacks of CefLoadHandler in the following cases:
 * 1. Before showing UI (before frame.setVisible(true) is called)
 * 2. With UI  (after frame.setVisible(true) was called)
 * 3. After disable showing UI (after frame.setVisible(false) was called)
 */
@ExtendWith(TestSetupExtension.class)
public class LoadPageWithoutUITest {
    private static final String DUMMY = "file://" + System.getProperty("test.src") + "/dummy.html";
    private static final String BLANK = "about:blank";

    private CountDownLatch latch;
    private JBCefBrowser browser;
    private JFrame frame = new JFrame("JCEF LoadPageWithoutUITest");

    private volatile boolean loadHandlerUsed;

    LoadPageWithoutUITest() {
        browser = new JBCefBrowser(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                CefLog.Info("onLoadingStateChange " + browser.getURL());
                loadHandlerUsed = true;
            }

            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                CefLog.Info("onLoadStart " + browser.getURL());
                loadHandlerUsed = true;
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                CefLog.Info("onLoadEnd " + browser.getURL());
                loadHandlerUsed = true;
                latch.countDown();
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                CefLog.Info("onLoadError " + browser.getURL());
                loadHandlerUsed = true;
            }
        });
    }

    public void initUI() {
        frame.getContentPane().add(browser.getComponent());
        frame.setSize(640, 480);
    }

    @Test
    public void test() throws InvocationTargetException, InterruptedException {
        if (OsrSupport.isEnabled()) {
            // Disable test because it has no sense for OSR: native browser doesn't know anything about
            // swing window visibility (it will always render page until browser closed).
            CefLog.Info("Skip LoadPageWithoutUITest because of OSR mode.");
            return;
        }

        if ("linux".equalsIgnoreCase(System.getProperty("os.name"))) {
            // Disable test because it can cause unstable jcef behaviour.
            // Details in: https://youtrack.jetbrains.com/issue/JBR-5200/Intermittent-LoadPageWithoutUITest-failures#focus=Comments-27-6798853.0-0
            CefLog.Info("Skip LoadPageWithoutUITest because of JBR-5200");
            return;
        }

        LoadPageWithoutUITest test = new LoadPageWithoutUITest();
        try {
            SwingUtilities.invokeAndWait(test::initUI);
            test.browser.getCefBrowser().createImmediately();

            CefLog.Info("Loading URL " + BLANK + " before enabling browser UI...");
            test.latch = new CountDownLatch(1);
            test.browser.loadURL(BLANK);
            test.latch.await(5, TimeUnit.SECONDS);
            if (!test.loadHandlerUsed) {
                throw new RuntimeException(BLANK + " is not loaded without browser UI");
            }
            test.loadHandlerUsed = false;
            CefLog.Info(BLANK + " is loaded");

            CefLog.Info("Loading URL " + DUMMY + " after enabling browser UI...");
            SwingUtilities.invokeAndWait(() -> test.frame.setVisible(true));
            test.latch = new CountDownLatch(1);
            test.browser.loadURL(DUMMY);
            test.latch.await(5, TimeUnit.SECONDS);
            if (!test.loadHandlerUsed) {
                throw new RuntimeException(DUMMY + " is not loaded with browser UI");
            }
            test.loadHandlerUsed = false;
            CefLog.Info(DUMMY + " is loaded");

            CefLog.Info("Loading URL " + BLANK + " after disabling browser UI...");
            SwingUtilities.invokeAndWait(() -> test.frame.setVisible(false));
            test.latch = new CountDownLatch(1);
            test.browser.loadURL(BLANK);
            test.latch.await(5, TimeUnit.SECONDS);
            if (!test.loadHandlerUsed) {
                throw new RuntimeException(DUMMY + " is not loaded after disabling browser UI");
            }
            test.loadHandlerUsed = false;
            CefLog.Info(BLANK + " is loaded");
        } finally {
            test.browser.dispose();
            test.browser.awaitClientDisposed();
            test.frame.dispose();
        }
    }
}