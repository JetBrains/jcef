package tests.junittests;

import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;

public class LoggingLoadHandler implements CefLoadHandler {
    final private CefSettings.LogSeverity myLevel;

    public LoggingLoadHandler(CefSettings.LogSeverity myLevel) {
        this.myLevel = myLevel;
    }

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        CefLog.Log(myLevel, "onLoadingStateChange, isLoading=" + isLoading + ", [native] id=" + browser.getIdentifier());
    }
    @Override
    public void onLoadStart(CefBrowser browser, CefFrame cefFrame, CefRequest.TransitionType transitionType) {
        CefLog.Log(myLevel, "onLoadStart, [native] id=" + browser.getIdentifier());
    }
    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame cefFrame, int httpStatusCode) {
        CefLog.Log(myLevel, "onLoadEnd, [native] id=" + browser.getIdentifier() + ", statusCode=" + httpStatusCode);
    }
    @Override
    public void onLoadError(CefBrowser browser, CefFrame cefFrame, ErrorCode errorCode, String errorText, String failedUrl) {
        CefLog.Log(myLevel, "onLoadError, [native] id=" + browser.getIdentifier() + ", errorText='" + errorText + "', url=" + failedUrl);
    }
}
