package tests.remote;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefCookieAccessFilter;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.PersistentHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.CefLog;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.cef.network.CefURLRequest;

public class TestResourceRequestHandler implements CefResourceRequestHandler, PersistentHandler {
    @Override
    public CefCookieAccessFilter getCookieAccessFilter(CefBrowser browser, CefFrame frame, CefRequest request) {
        return new TestCookieAccessFilter();
    }

    @Override
    public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
        CefLog.Info("onBeforeResourceLoad " + browser + ", request:" + request);
        return false;
    }

    @Override
    public CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame, CefRequest request) {
        // TODO: implement and test
        CefLog.Error("Test-Unimplemented: getResourceHandler.");
        return null;
    }

    @Override
    public void onResourceRedirect(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, StringRef new_url) {
        CefLog.Info("onResourceRedirect " + browser + ", request:" + request + ", response:" + response);
    }

    @Override
    public boolean onResourceResponse(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response) {
        CefLog.Info("onResourceResponse " + browser + ", request:" + request + ", response:" + response);
        return false;
    }

    @Override
    public void onResourceLoadComplete(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, CefURLRequest.Status status, long receivedContentLength) {
        CefLog.Info("onResourceLoadComplete " + browser + ", request:" + request + ", response:" + response);
    }

    @Override
    public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request, BoolRef allowOsExecution) {
        CefLog.Info("onProtocolExecution " + browser + ", request:" + request);
    }
}
