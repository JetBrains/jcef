package tests;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;
import tests.junittests.TestFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// This test is used to debug crash JBR-6864
public class LoopTest {
    private static final int LOOP_COUNT = Utils.getInteger("jcef.tests.loop_count", 100000);
    private static final String ourTestUrl = "http://test.com/test.html";

    public static void main(String[] args) throws InterruptedException {
        String logfile = "loop_test.log";
        System.setProperty("CEF_SERVER_LOG_PATH", logfile);
        CefLog.init(logfile, CefSettings.LogSeverity.LOGSEVERITY_VERBOSE);
        if (!CefApp.isRemoteEnabled()) {
            CefLog.Debug("Skip LoopTest.");
            return;
        }

        CefApp.startup(args);

        JCefAppConfig config = JCefAppConfig.getInstance();
        List<String> appArgs = new ArrayList<>(Arrays.asList(args));
        appArgs.addAll(config.getAppArgsAsList());
        args = appArgs.toArray(new String[0]);

        CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
        });

        CefSettings settings = config.getCefSettings();
        settings.log_file = "loop_test_chromium.log";
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_INFO;

        CefApp app = CefApp.getInstance(settings);

        {// Wait for initialization
            CountDownLatch latch = new CountDownLatch(1);
            app.onInitialization(s -> latch.countDown());
            waitLatch(latch, 10000, "CefApp wasn't initialized");
        }

//        testImmediateClose();
//        Thread.sleep(2000);
//        testFastClose();
//        Thread.sleep(2000);
        testCloseAfterCreated();
        Thread.sleep(2000);
        testCloseAfterPageLoaded();
        Thread.sleep(2000);
        testLoadAndResize();

        app.dispose();
        CefLog.Info("LoopTest is finished!");
    }

    private static void testImmediateClose() {
        CefLog.Info("Test immediate close (direct dispose).");
        for (int c = 0; c < LOOP_COUNT / 10; ++c) {
            CefLog.Debug("====== Iteration %d ======", c);
            MyTestFrame frame = new MyTestFrame();
            frame.dispose();
        }
        // Positive test result: no crashes
    }

    private static void testFastClose() {
        CefLog.Info("Test immediate close via dispatch WINDOW_CLOSING (it tests closing directly after native creation started).");
        for (int c = 0; c < LOOP_COUNT / 10; ++c) {
            CefLog.Debug("====== Iteration %d ======", c);
            MyTestFrame frame = new MyTestFrame();
            SwingUtilities.invokeLater(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
        }
        // Positive test result: no crashes
    }

    private static void testCloseAfterCreated() {
        CefLog.Info("Test closing after native browser created.");
        for (int c = 0; c < LOOP_COUNT / 10; ++c) {
            CefLog.Debug("====== Iteration %d ======", c);
            MyTestFrame frame = new MyTestFrame() {
                @Override
                public void onAfterCreated(CefBrowser browser) {
                    terminateTest();
                }
            };
            frame.awaitCompletion();
        }
        // Positive test result: (1) no crashes, (2) called callbacks: onAfterCreated
    }

    private static void testCloseAfterPageLoaded() {
        CefLog.Info("Test closing after page loaded.");
        Random rnd = new Random(System.nanoTime());
        for (int c = 0; c < LOOP_COUNT / 5; ++c) {
            CefLog.Debug("====== Iteration %d ======", c);
            MyTestFrame frame = new MyTestFrame() {
                @Override
                public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                    if (!isLoading) {
                        int delay = rnd.nextInt(20);
                        Timer t = new Timer(delay, e -> {
                            terminateTest();
                        });
                        t.setRepeats(false);
                        t.start();
                    }
                }
            };
            frame.awaitCompletion();
        }
        // Positive test result: (1) no crashes, (2) called callbacks: onLoadingStateChange
    }

    private static void testLoadAndResize() throws InterruptedException {
        CefLog.Info("Test closing after some long work (loading and resize).");
        for (int c = 0; c < LOOP_COUNT; ++c) {
            CefLog.Debug("====== Iteration %d ======", c);
            final CountDownLatch l[] = new CountDownLatch[]{new CountDownLatch(1)};
            MyTestFrame frame = new MyTestFrame() {
                @Override
                public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                    if (!isLoading) {
                        l[0].countDown();
                    }
                }
            };
            CefLog.Debug("Resize 500, 300.");
            frame.setSize(500, 300);
            l[0] = new CountDownLatch(1);
            //String url = "https://www.google.com/maps";
            //frame.loadURL(url);
            waitLatch(l[0], 100000, String.format("Can't load '%s'", ourTestUrl));

            CefLog.Debug("Resize 800, 600.");
            frame.setSize(800, 600);
            Thread.sleep(700);

            l[0] = new CountDownLatch(1);
            String url = "https://www.youtube.com/";
            frame.loadURL(url);
            waitLatch(l[0], 100000, String.format("Can't load '%s'", url));

            CefLog.Debug("Resize 1500, 1000.");
            frame.setSize(1500, 1000);
            Thread.sleep(700);

            CefLog.Debug("Close browser.");
            SwingUtilities.invokeLater(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
            frame.awaitCompletion();
        }
        // Positive test result: (1) no crashes
    }

    private static void waitLatch(CountDownLatch latch, String errDesc) throws InterruptedException {
        waitLatch(latch, 2000, errDesc);
    }

    private static void waitLatch(CountDownLatch latch, int timeoutMs, String errDesc) throws InterruptedException {
        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            CefLog.Error("InterruptedException: %s", e.getMessage());
            throw e;
        }
        if (latch.getCount() > 0) {
            CefLog.Error("Wait timeout (%d ms) elapsed. Error: %s", timeoutMs, errDesc);
            throw new RuntimeException(errDesc);
        }
    }

    static class MyTestFrame extends TestFrame {
        @Override
        protected void setupTest() {
            addResource(ourTestUrl, "<html><body>Test!</body></html>", "text/html");
            createBrowser(ourTestUrl); // NOTE: calls JFrame.setVisible internally
        }
        void loadURL(String url) { browser_.loadURL(url); }
        public void dispose() {
            browser_.close(true);
            client_.dispose();
            super.dispose();
        }
        protected void createBrowser(String startURL) {
            assertNull(browser_);
            browser_ = OsrSupport.createBrowser(client_, startURL);
            assertNotNull(browser_);

            SwingUtilities.invokeLater(()->{
                getContentPane().add(browser_.getUIComponent(), BorderLayout.CENTER);
                pack(); // NOTE: if remove pack() then multipleBrowserCreation runs correctly
                setSize(800, 600);
                setVisible(true);
            });
        }
    }
}
