package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @requires (os.arch == "amd64" | os.arch == "x86_64" | (os.arch == "aarch64" & os.family == "mac"))
 * @summary Tests that JCEF starts and loads empty page with no crash
 * @author Anton Tarasov
 */
@ExtendWith(TestSetupExtension.class)
public class JCEFStartupTest {
    private static class TestFrame {
        final CountDownLatch myLatch = new CountDownLatch(1);
        volatile boolean isPassed;
        volatile JBCefBrowser myBrowser;
        private JFrame myFrame;

        TestFrame() {
            myBrowser = new JBCefBrowser(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                    CefLog.Info("onLoadingStateChange:" + browser);
                }

                @Override
                public void onLoadStart(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest.TransitionType transitionType) {
                    CefLog.Info("onLoadStart:" + cefBrowser);
                }

                @Override
                public void onLoadEnd(CefBrowser cefBrowser, CefFrame cefFrame, int i) {
                    CefLog.Info("onLoadEnd:" + cefBrowser);
                    isPassed = true;
                    myLatch.countDown();
                }

                @Override
                public void onLoadError(CefBrowser cefBrowser, CefFrame cefFrame, ErrorCode errorCode, String s, String s1) {
                    CefLog.Info("onLoadError:" + cefBrowser);
                }
            });
            CefLog.Info("created browser:" + myBrowser.getCefBrowser());
        }

        public void initUI() {
            myFrame = new JFrame("JCEF");
            myFrame.add(myBrowser.getComponent());

            myFrame.setSize(640, 480);
            myFrame.setLocationRelativeTo(null);
            myFrame.setVisible(true);
        }

        public void dispose(boolean disposeBrowser) {
            if (disposeBrowser) myBrowser.dispose();
            if (myFrame != null) {
                myFrame.dispose();
                myFrame = null;
            }
        }
    }

    @Test
    public void test() {
        _test(true);
    }
    @Test
    public void testJBR2222() {
        _test(false);
    }

    @Test
    public void testCreation10Times() throws InterruptedException {
        // Test CefLoadHandlerAdapter callbacks invocation
        final long count = 10;
        CefLog.Info("Start testCreation10Times");
        for (long c = 0; c < count; ++c) {
            TestFrame frame = new TestFrame();
            EventQueue.invokeLater(() -> frame.initUI());

            try {
                frame.myLatch.await(50, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            frame.dispose(true);

            if (!frame.isPassed) {
                throw new RuntimeException("FAILED testCreation10Times: CefLoadHandler.onLoadEnd wasn't invoked, iteration " + c);
            }
        }
        CefLog.Info("Test PASSED");
    }
    private void _test(boolean disposeBrowser) {
        TestFrame frame = new TestFrame();
        EventQueue.invokeLater(() -> frame.initUI());

        try {
            frame.myLatch.await(50, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        frame.dispose(disposeBrowser);

        if (!frame.isPassed) {
            throw new RuntimeException("Test FAILED!");
        }
        CefLog.Info("Test PASSED");
    }
}
