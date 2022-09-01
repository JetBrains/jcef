package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
public class LoadPageWithoutUI {
    private static final String DUMMY = "file://" + System.getProperty("test.src") + "/dummy.html";
    private static final String BLANK = "about:blank";

    private CountDownLatch latch;
    private JBCefBrowser browser;
    private JFrame frame = new JFrame("JCEF");

    private volatile boolean loadHandlerUsed;

    LoadPageWithoutUI() {
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
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                browser.dispose();
            }
        });
    }

    @Test
    public void test() throws InvocationTargetException, InterruptedException {
        LoadPageWithoutUI test = new LoadPageWithoutUI();
        try {
            test.latch = new CountDownLatch(1);
            SwingUtilities.invokeLater(test::initUI);
            test.latch.await(5, TimeUnit.SECONDS);

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
            SwingUtilities.invokeAndWait(() -> test.frame.dispose());
        }
    }
}