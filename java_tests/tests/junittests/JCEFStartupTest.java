package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.*;
import java.awt.*;
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

        public void dispose() {
            myBrowser.dispose();
            if (myFrame != null) {
                myFrame.dispose();
                myFrame = null;
            }
            myBrowser.awaitClientDisposed();
        }
    }

    @Test
    public void test() {
        testCreation(1);
    }

    @Test
    public void testCreation100Times() throws InterruptedException {
        testCreation(100);
    }

    @Test
    // Reproducer for JBR-4872 (use with long timeout > 4 hours)
    public void testCreationWithTimeout() throws InterruptedException {
        String stime = System.getenv().get("JCEF_JUNIT_STARTUP_TIMEOUT_MIN");
        if (stime == null || stime.isEmpty()) return;
        long durationMin;
        try {
            durationMin = Integer.parseInt(stime);
        } catch (NumberFormatException e) {
            CefLog.Warn("skip testCreationWithTimeout, exception during parse env variable", e.getMessage());
            return;
        }

        final long startMs = System.currentTimeMillis();
        CefLog.Info("Start testCreation, timeout=%d min", durationMin);
        int c = 0;
        while (System.currentTimeMillis() - startMs < durationMin*60*1000){
            CefLog.Info("=== iteraion %d ===", c++);
            testCreation();
        }
        CefLog.Info("Test PASSED");
    }

    private void testCreation(int count) {
        CefLog.Info("Start testCreation, count=%d", count);
        for (long c = 0; c < count; ++c) {
            CefLog.Info("=== iteraion %d ===", c);
            testCreation();
        }
        CefLog.Info("Test PASSED");
    }

    private void testCreation() {
        // Test CefLoadHandlerAdapter callbacks invocation and cefclient disposing
        TestFrame frame = new TestFrame();
        EventQueue.invokeLater(() -> frame.initUI());

        try {
            frame.myLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            CefLog.Error(e.getMessage());
            e.printStackTrace();
        }

        frame.dispose();

        if (!frame.isPassed) {
            CefLog.Error("CefLoadHandler.onLoadEnd wasn't invoked");
            throw new RuntimeException("CefLoadHandler.onLoadEnd wasn't invoked");
        }
    }
}
