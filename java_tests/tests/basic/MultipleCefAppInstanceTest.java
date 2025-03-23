package tests.basic;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.misc.CefLog;
import tests.OsrSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MultipleCefAppInstanceTest {

    private void create(CefApp cefApp, String startURL, CountDownLatch testLatch) {
        CefClient client = cefApp.createClient();
        final CountDownLatch created = new CountDownLatch(1);
        client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                CefLog.Info("Created browser '%s' with URL: %s", browser, browser.getURL());
                testLatch.countDown();
                created.countDown();
            }
            @Override
            public void onBeforeClose(CefBrowser browser) {
                CefLog.Info("Closed browser '%s'", browser);
                testLatch.countDown();
            }
        });

        CefBrowser browser = OsrSupport.createBrowser(client, startURL);
        browser.createImmediately();
        try {
            created.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        if (created.getCount() == 0)
            browser.close(true);
        else
            CefLog.Error("Elapsed timeout of browser creation: '%s' with URL %s", browser, browser.getURL());
    }

    // TODO: test with different appHandlers
    void testMultipleInstances() {
        if (!CefApp.isRemoteEnabled())
            return;

        String[] args = new String[0]; // initial list is empty

        JCefAppConfig config = JCefAppConfig.getInstance();
        List<String> appArgs = new ArrayList<>(Arrays.asList(args));
        appArgs.addAll(config.getAppArgsAsList());
        args = appArgs.toArray(new String[0]);

        CountDownLatch test1 = new CountDownLatch(2);
        CefSettings settings1 = config.getCefSettings().clone();
        settings1.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        //settings1.log_file = "/Users/bocha/jcef_test1.log";
        CefApp cefApp1 = CefApp.getInstance(args, settings1);
        create(cefApp1, "http://www.google.com", test1);

        CountDownLatch test2 = new CountDownLatch(2);
        CefSettings settings2 = config.getCefSettings();
        settings2.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        //settings2.log_file = "/Users/bocha/jcef_test2.log";
        appArgs.add("-fake-test-argument");
        args = appArgs.toArray(new String[0]);
        CefApp cefApp2 = CefApp.getInstance(args, settings2);
        create(cefApp2, "http://www.ya.ru", test2);

        try {
            test1.await(30, TimeUnit.SECONDS);
            test2.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            CefLog.Error("Exception: %s", e.getMessage());
            return;
        }
        if (test1.getCount() > 0 || test2.getCount() > 0) {
            CefLog.Error("Multiple CefApp instances test was FAILED: lc1=%d, lc2=%d.");
            return;
        }
        CefLog.Info("Multiple CefApp instances test was successfully finished.");
    }


    public static void main(String[] args) {
        // Init VERBOSE java logging
//        final CefSettings.LogSeverity logLevel = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
//        CefLog.init(null, logLevel);
//
//        // Init VERBOSE native logging to the same stream
//        System.setProperty("CEF_SERVER_LOG_LEVEL", "VERBOSE");
//
//        // Init vmodule chromium logging to the same stream
//        System.setProperty("JCEF_TESTS_LOG_LEVEL", "info");
//        System.setProperty("JCEF_TESTS_EXTRA_ARGS", "--enable-logging=stderr;--vmodule=statistics_recorder*=0;--v=2");

        new MultipleCefAppInstanceTest().testMultipleInstances();
    }
}
