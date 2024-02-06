package tests.remote;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefCookieAccessFilter;
import org.cef.misc.CefLog;
import org.cef.network.CefCookie;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

public class TestCookieAccessFilter implements CefCookieAccessFilter {
    @Override
    public boolean canSendCookie(CefBrowser browser, CefFrame frame, CefRequest request, CefCookie cookie) {
        CefLog.Info("canSendCookie " + browser + ", request:" + request);
        return false;
    }

    @Override
    public boolean canSaveCookie(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, CefCookie cookie) {
        CefLog.Info("canSaveCookie " + browser + ", request:" + request + ", response:" + response);
        return false;
    }
}
