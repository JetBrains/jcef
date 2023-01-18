package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.misc.CefLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests that JCEF correctly loads empty page several times
 * (checks that CefLoadHandler.onLoadEnd invocation and cefclient disposing)
 */
@ExtendWith(TestSetupExtension.class)
public class MultipleBrowserCreationTest {
    private static class TestFrame {
        final CountDownLatch myOnLoadEndLatch = new CountDownLatch(1);
        volatile JBCefBrowser myBrowser;
        private JFrame myFrame;

        TestFrame() {
            myBrowser = new JBCefBrowser(new LoggingLoadHandler(CefSettings.LogSeverity.LOGSEVERITY_INFO) {
                @Override
                public void onLoadEnd(CefBrowser browser, CefFrame cefFrame, int i) {
                    super.onLoadEnd(browser, cefFrame, i);
                    myOnLoadEndLatch.countDown();
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
            myBrowser.awaitClientDisposed();
            if (myFrame != null) {
                myFrame.dispose();
                myFrame = null;
            }
        }
    }

    @Test
    public void testCreation20Times() {
        CefLog.Info("Start MultipleBrowserCreation with 20 iterations");
        for (long c = 0; c < 20; ++c) {
            CefLog.Info("=== iteraion %d ===", c);
            _test();
        }
        CefLog.Info("Test PASSED");
    }

    @Test
    // Reproducer for JBR-4872 (use with long timeout > 4 hours)
    public void testWithTimeout() {
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
        CefLog.Info("Start MultipleBrowserCreation with timeout=%d min", durationMin);
        int c = 0;
        while (System.currentTimeMillis() - startMs < durationMin*60*1000){
            CefLog.Info("=== iteraion %d ===", c++);
            _test();
        }
        CefLog.Info("Test PASSED");
    }

    private void _test() {
        TestFrame frame = new TestFrame();
        EventQueue.invokeLater(() -> frame.initUI());
        frame.myBrowser.awaitBrowserCreated();

        try {
            frame.myOnLoadEndLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            CefLog.Error(e.getMessage());
            e.printStackTrace();
        }

        frame.dispose();

        if (frame.myOnLoadEndLatch.getCount() > 0) {
            CefLog.Error("CefLoadHandler.onLoadEnd wasn't invoked");
            throw new RuntimeException("CefLoadHandler.onLoadEnd wasn't invoked");
        }
    }
}
