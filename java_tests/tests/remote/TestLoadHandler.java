package tests.remote;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;

public class TestLoadHandler implements CefLoadHandler {
    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        CefLog.Info("onLoadingStateChange " + browser + " " + isLoading + ", " + canGoBack + ", " + canGoForward);
    }

    @Override
    public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
        CefLog.Info("onLoadStart " + browser + ", " + transitionType);
    }

    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        CefLog.Info("onLoadEnd " + browser + ", " + httpStatusCode);
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
        CefLog.Info("onLoadError " + browser + ", " + errorCode + ", " + errorText);
    }
}
