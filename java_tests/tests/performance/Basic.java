package tests.performance;

import com.jetbrains.cef.remote.NativeServerManager;
import com.jetbrains.cef.remote.ThriftTransport;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;
import tests.CefInitHelper;
import tests.OsrSupport;
import tests.junittests.LoggingLifeSpanHandler;
import tests.junittests.LoggingLoadHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Basic {
    //
    // Measure JCEF-related times
    //
    public static void main(String[] args) {
        final long timeStartNs = System.nanoTime();
        CefInitHelper.initializeCef();
        final boolean isRemote = CefApp.isRemoteEnabled();

        CountDownLatch appInitializationLatch = new CountDownLatch(1);
        CefApp.getInstance().onInitialization(state -> {
            if (state == CefApp.CefAppState.INITIALIZED) {
                final long spentCefAppInitializationMs = (System.nanoTime() - timeStartNs)/1000000l;
                appInitializationLatch.countDown();
                CefLog.Info("CefApp successfully initialized, spent %d ms", spentCefAppInitializationMs);
                _sendStatisticValue(isRemote ? "timeCefAppInitializationRemoteMs" : "timeCefAppInitializationMs", spentCefAppInitializationMs);
            }
        });

        _wait(appInitializationLatch, 5, "CefApp wasn't initialized");

        final long timeAfterInitNs = System.nanoTime();

        CefClient client = CefApp.getInstance().createClient();

        CountDownLatch onAfterCreated_ = new CountDownLatch(1);
        client.addLifeSpanHandler(new LoggingLifeSpanHandler(CefSettings.LogSeverity.LOGSEVERITY_INFO) {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                final long spentBrowserCreationMs = (System.nanoTime() - timeAfterInitNs)/1000000l;
                onAfterCreated_.countDown();
                CefLog.Info("Native browser creation spent %d ms", spentBrowserCreationMs);
                _sendStatisticValue(isRemote ? "timeOnAfterCreatedRemoteMs" : "timeOnAfterCreatedMs", spentBrowserCreationMs);
            }
        });
        CountDownLatch onLoadStartLatch = new CountDownLatch(1);
        CountDownLatch onLoadEndLatch = new CountDownLatch(1);
        CountDownLatch onLoadErrLatch = new CountDownLatch(1);
        client.addLoadHandler(new LoggingLoadHandler(CefSettings.LogSeverity.LOGSEVERITY_INFO) {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame cefFrame, CefRequest.TransitionType transitionType) {
                final long spentUntilLoadStartedMs = (System.nanoTime() - timeAfterInitNs)/1000000l;
                onLoadStartLatch.countDown();
                CefLog.Info("Until onLoadStart spent %d ms", spentUntilLoadStartedMs);
                _sendStatisticValue(isRemote ? "timeOnLoadStartRemoteMs" : "timeOnLoadStartMs", spentUntilLoadStartedMs);
            }
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame cefFrame, int i) {
                final long spentUntilLoadEndMs = (System.nanoTime() - timeAfterInitNs)/1000000l;
                onLoadEndLatch.countDown();
                CefLog.Info("Until onLoadEnd spent %d ms", spentUntilLoadEndMs);
                _sendStatisticValue(isRemote ? "timeOnLoadEndRemoteMs" : "timeOnLoadEndMs", spentUntilLoadEndMs);
            }
            @Override
            public void onLoadError(CefBrowser browser, CefFrame cefFrame, ErrorCode errorCode, String errorText, String failedUrl) {
                CefLog.Debug("onLoadErr: %s - %s - %s", errorCode, errorText, failedUrl);
                onLoadErrLatch.countDown();
            }
        });

        CefBrowser browser;
        if (OsrSupport.isEnabled()) {
            browser = OsrSupport.createBrowser(client, "about:blank");
            browser.createImmediately();
        } else {
            browser = client.createBrowser("about:blank", false, false);
        }

        try {
            _wait(onAfterCreated_, 5, "Native CefBrowser wasn't created");
            try {
                _wait(onLoadStartLatch, 5, "onLoadStart wasn't called, [native] id="+browser.getIdentifier());
            } catch (RuntimeException e) {
                if (onLoadErrLatch.getCount() <= 0) {
                    // empiric observation: onLoadStart can be skipped when onLoadError occured.
                    // see https://youtrack.jetbrains.com/issue/JBR-5192/Improve-JCEF-junit-tests#focus=Comments-27-6799179.0-0
                    CefLog.Info("onLoadStart wasn't called and onLoadError was observed");
                } else throw e;
            }
            _wait(onLoadEndLatch, 10, "onLoadEnd wasn't called");
        } finally {
            if (browser != null) {
                browser.setCloseAllowed();
                browser.close(true);
            }
            client.dispose();

            // dispose CefApp
            CefInitHelper.shutdonwCef();
            if (CefApp.isRemoteEnabled()) {
                // Ensure that server process is stopped
                boolean stopped = NativeServerManager.waitForStopped(ThriftTransport.ourDefaultServer, 15000);
                if (!stopped)
                    CefLog.Error("Can't stop server in %d ms.", 15000);
            }
        }
    }

    private static void _wait(CountDownLatch latch, int timeoutSec, String errorDesc) {
        try {
            if (!latch.await(timeoutSec, TimeUnit.SECONDS)) {
                CefLog.Error(errorDesc);
                throw new RuntimeException(errorDesc);
            }
        } catch (InterruptedException e) {
            CefLog.Error(e.getMessage());
        }
    }

    private static void _sendStatisticValue(String key, long value) {
        System.out.printf("##teamcity[buildStatisticValue key='%s' value='%d']\n", key, value);
        System.out.flush();
    }
}
