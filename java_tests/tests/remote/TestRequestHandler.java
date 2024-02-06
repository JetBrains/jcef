package tests.remote;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandler;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;
import org.cef.security.CefSSLInfo;

public class TestRequestHandler implements CefRequestHandler {
    @Override
    public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
        CefLog.Info("onBeforeBrowse " + browser + ", user_gesture=" + user_gesture);
//                Map<String, String> map = new HashMap<>();
//                request.getHeaderMap(map);
//                CefLog.Info("\trequest header map: ", map.toString());
//                CefPostData pd = request.getPostData();
//                CefLog.Info("\trequest post data: ", pd.toString());
//                CefLog.Info("\trequest: %s", CefRequest.toString(request));
        return false;
    }

    @Override
    public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String target_url, boolean user_gesture) {
        CefLog.Info("onOpenURLFromTab " + browser + ", " + target_url);
        return false;
    }

    @Override
    public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame, CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator, BoolRef disableDefaultHandling) {
        CefLog.Info("getResourceRequestHandler " + browser + ", " + requestInitiator);
        CefLog.Info("\trequest:" + request);
        return new TestResourceRequestHandler();
    }

    @Override
    public boolean getAuthCredentials(CefBrowser browser, String origin_url, boolean isProxy, String host, int port, String realm, String scheme, CefAuthCallback callback) {
        CefLog.Info("getAuthCredentials " + browser + ", " + origin_url);
        //callback.Continue("test_user", "test_password"); // TODO: schedule later
        return false;
        //return true;
    }

    @Override
    public boolean onCertificateError(CefBrowser browser, CefLoadHandler.ErrorCode cert_error, String request_url, CefSSLInfo sslInfo, CefCallback callback) {
        CefLog.Info("onCertificateError " + browser + ", err=" + cert_error);
        //callback.Continue(); // TODO: schedule later
        return false;
        //return true;
    }

    @Override
    public void onRenderProcessTerminated(CefBrowser browser, TerminationStatus status) {
        CefLog.Info("onRenderProcessTerminated " + browser + ", status=" + status);
    }
}
