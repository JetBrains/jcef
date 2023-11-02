package tests.remote;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.misc.CefLog;

public class TestMessageRouterHandler implements CefMessageRouterHandler {
    @Override
    public long getNativeRef(String identifer) {
        return 0;
    }

    @Override
    public void setNativeRef(String identifer, long nativeRef) {}

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        CefLog.Info("TestMessageRouterHandler.onQuery: handler query in browser %s, qid=%d, request=%s", browser, queryId, request);
        return false;
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        CefLog.Info("TestMessageRouterHandler.onQueryCanceled: browser %s, qid=%d", browser, queryId);
    }
}
