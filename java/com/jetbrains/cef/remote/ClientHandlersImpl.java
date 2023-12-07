package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.callback.RemoteAuthCallback;
import com.jetbrains.cef.remote.callback.RemoteCallback;
import com.jetbrains.cef.remote.network.*;
import com.jetbrains.cef.remote.router.RemoteMessageRouterHandler;
import com.jetbrains.cef.remote.router.RemoteQueryCallback;
import com.jetbrains.cef.remote.thrift_codegen.*;
import com.jetbrains.cef.remote.thrift_codegen.Point;
import com.jetbrains.cef.remote.thrift_codegen.Rect;
import com.jetbrains.cef.remote.thrift_codegen.ScreenInfo;
import org.apache.thrift.TException;
import org.cef.CefSettings;
import org.cef.browser.CefFrame;
import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefCallback;
import org.cef.handler.*;
import org.cef.misc.BoolRef;
import org.cef.misc.CefLog;
import org.cef.misc.StringRef;
import org.cef.network.CefCookie;
import org.cef.network.CefRequest;
import org.cef.network.CefURLRequest;
import org.cef.security.CefSSLInfo;

import java.awt.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//
// Service for rpc from native to java
//
public class ClientHandlersImpl implements ClientHandlers.Iface, RemoteClient.BrowserTracker {
    private final Map<Integer, RemoteBrowser> myBid2RemoteBrowser = new ConcurrentHashMap<>();
    private final RemoteApp myRemoteApp;
    private final RpcExecutor myServer;

    private static final CefFrame NULL_FRAME = new RemoteFrame();

    public ClientHandlersImpl(RpcExecutor server, RemoteApp remoteApp) {
        myRemoteApp = remoteApp;
        myServer = server;
    }

    @Override
    public void register(RemoteBrowser browser) {
        if (browser.getBid() < 0) {
            CefLog.Error("Can't register browser with bid %d", browser.getBid());
            return;
        }
        myBid2RemoteBrowser.put(browser.getBid(), browser);
    }

    @Override
    public void unregister(int bid) {
        RemoteBrowser browser = myBid2RemoteBrowser.remove(bid);
        if (browser == null)
            CefLog.Error("unregisterBrowser: bid=%d was already removed.");
    }

    private RemoteBrowser getRemoteBrowser(int bid) {
        RemoteBrowser browser = myBid2RemoteBrowser.get(bid);
        if (browser == null) {
            CefLog.Error("Can't find remote browser with bid=%d.", bid);
            return null;
        }
        return browser;
    }

    @Override
    public int connect() {
        return 0;
    }

    @Override
    public void log(String msg) {
        CefLog.Debug("received message from CefServer: " + msg);
    }

    //
    // CefApp
    //

    @Override
    public void AppHandler_OnContextInitialized() {
        CefLog.Debug("AppHandler_OnContextInitialized: ");
        myRemoteApp.onContextInitialized();
    }

    @Override
    public List<CustomScheme> AppHandler_GetRegisteredCustomSchemes() {
        CefLog.Debug("AppHandler_GetRegisteredCustomSchemes: ");
        return myRemoteApp.getAllRegisteredCustomSchemes();
    }

    //
    // CefRenderHandler
    //

    private static final Rect INVALID_RECT = new Rect(0,0,-1,-1);
    private static final Point INVALID_POINT = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private static final ScreenInfo INVALID_SCREENINFO = new ScreenInfo(-1, -1, -1, false, new Rect(), new Rect());

    // NOTE: assume getRenderHandler() != null always

    @Override
    public Rect RenderHandler_GetViewRect(int bid) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return INVALID_RECT;
        RemoteClient rclient = browser.getOwner();
        CefRenderHandler rh = rclient.getRenderHandler();
        Rectangle rect = rh.getViewRect(browser);
        return new Rect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public ScreenInfo RenderHandler_GetScreenInfo(int bid) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return INVALID_SCREENINFO;
        RemoteClient rclient = browser.getOwner();
        CefRenderHandler rh = rclient.getRenderHandler();
        CefScreenInfo csi = new CefScreenInfo();
        boolean success = rh.getScreenInfo(browser, csi);
        return success ?
             new ScreenInfo(
                csi.device_scale_factor,
                csi.depth,
                csi.depth_per_component,
                csi.is_monochrome,
                new Rect(csi.x, csi.y, csi.width, csi.height),
                new Rect(csi.available_x, csi.available_y, csi.available_width, csi.available_height))
             : INVALID_SCREENINFO;
    }

    @Override
    public Point RenderHandler_GetScreenPoint(int bid, int viewX, int viewY) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return INVALID_POINT;
        RemoteClient rclient = browser.getOwner();
        CefRenderHandler rh = rclient.getRenderHandler();
        java.awt.Point res = rh.getScreenPoint(browser, new java.awt.Point(viewX, viewY));
        return new Point(res.x, res.y);
    }

    @Override
    public void RenderHandler_OnPaint(int bid, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, boolean recreateHandle, int width, int height) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        RemoteClient rc = browser.getOwner();
        CefRenderHandler rh = rc.getRenderHandler();
        ((CefNativeRenderHandler)rh).onPaintWithSharedMem(browser, popup, dirtyRectsCount, sharedMemName, sharedMemHandle, recreateHandle, width, height);
    }

    //
    // CefLifeSpanHandler
    //

    @Override
    public boolean LifeSpanHandler_OnBeforePopup(int bid, String url, String frameName, boolean gesture) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        return browser.getOwner().hLifeSpan.handleBool(lsh-> lsh.onBeforePopup(browser, NULL_FRAME, url, frameName));
    }

    @Override
    public void LifeSpanHandler_OnAfterCreated(int bid) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        browser.getOwner().hLifeSpan.handle(lsh->lsh.onAfterCreated(browser));
    }

    /**
     * Called when a browser has received a request to close.
     *
     * If no OS window exists (window rendering disabled)
     * returning false will cause the browser object to be destroyed immediately. Return true if the
     * browser is parented to another window and that other window needs to receive close
     * notification via some non-standard technique.
     *
     * @param bid The browser generating the event.
     * @return False to send an OS close notification to the browser window's top-level owner.
     */
    @Override
    public boolean LifeSpanHandler_DoClose(int bid) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser != null)
            browser.getOwner().hLifeSpan.handle(lsh->lsh.doClose(browser));
        return false;
    }

    @Override
    public void LifeSpanHandler_OnBeforeClose(int bid) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        browser.getOwner().hLifeSpan.handle(lsh->lsh.onBeforeClose(browser));
    }


    //
    // CefLoadHandler
    //

    @Override
    public void LoadHandler_OnLoadingStateChange(int bid, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        browser.getOwner().getLoadHandler().onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
    }

    @Override
    public void LoadHandler_OnLoadStart(int bid, int transition_type) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        // TODO: use correct transition_type instead of TT_LINK
        browser.getOwner().getLoadHandler().onLoadStart(browser, NULL_FRAME, CefRequest.TransitionType.TT_LINK);
    }

    @Override
    public void LoadHandler_OnLoadEnd(int bid, int httpStatusCode) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        browser.getOwner().getLoadHandler().onLoadEnd(browser, NULL_FRAME, httpStatusCode);
    }

    @Override
    public void LoadHandler_OnLoadError(int bid, int errorCode, String errorText, String failedUrl) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        browser.getOwner().getLoadHandler().
                onLoadError(browser, NULL_FRAME, CefLoadHandler.ErrorCode.findByCode(errorCode), errorText, failedUrl
        );
    }

    //
    // CefDisplayHandler
    //

    @Override
    public void DisplayHandler_OnAddressChange(int bid, String url) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return;

        dh.onAddressChange(browser, NULL_FRAME, url);
    }

    @Override
    public void DisplayHandler_OnTitleChange(int bid, String title) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return;

        dh.onTitleChange(browser, title);
    }

    @Override
    public boolean DisplayHandler_OnTooltip(int bid, String text) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return false;

        return dh.onTooltip(browser, text);
    }

    @Override
    public void DisplayHandler_OnStatusMessage(int bid, String value) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return;

        dh.onStatusMessage(browser, value);
    }

    @Override
    public boolean DisplayHandler_OnConsoleMessage(int bid, int level, String message, String source, int line) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return false;

        // TODO: fix log level
        CefLog.Error("onConsoleMessage: used incorrect log level");
        return dh.onConsoleMessage(browser, CefSettings.LogSeverity.LOGSEVERITY_DEFAULT, message, source, line);
    }

    //
    // CefRequestHandler
    //

    @Override
    public boolean RequestHandler_OnBeforeBrowse(int bid, RObject request, boolean user_gesture, boolean is_redirect) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        RemoteRequestImpl rr = new RemoteRequestImpl(myServer, request);
        boolean result = rh.onBeforeBrowse(browser, NULL_FRAME, new RemoteRequest(rr), user_gesture, is_redirect);
        return result;
    }

    private static final RObject INVALID_PERSISTENT = new RObject(-1).setIsPersistent(true);

    @Override
    public RObject RequestHandler_GetResourceRequestHandler(int bid, RObject request, boolean isNavigation, boolean isDownload, String requestInitiator) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return INVALID_PERSISTENT;

        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return INVALID_PERSISTENT;

        RemoteRequestImpl rr = new RemoteRequestImpl(myServer, request);
        BoolRef disableDefaultHandling = new BoolRef(false);
        CefResourceRequestHandler handler = rh.getResourceRequestHandler(browser, NULL_FRAME, new RemoteRequest(rr), isNavigation, isDownload, requestInitiator, disableDefaultHandling);
        if (handler == null) return INVALID_PERSISTENT;

        boolean isPersistent = handler instanceof PersistentHandler;
        if (!isPersistent) {
            CefLog.Error("Non-persistent CefResourceRequestHandler can cause unstable behaviour and will not be used. Please use PersistentHandler.");
            return INVALID_PERSISTENT;
        }

        RemoteResourceRequestHandler resultHandler = RemoteResourceRequestHandler.create(handler);
        return resultHandler.thriftId(true, disableDefaultHandling.get());
    }

    @Override
    public RObject ResourceRequestHandler_GetCookieAccessFilter(int rrHandler, int bid, RObject request) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return INVALID_PERSISTENT;

        CefResourceRequestHandler handler = rrrh.getDelegate();
        RemoteRequestImpl rr = new RemoteRequestImpl(myServer, request);
        CefCookieAccessFilter filter = handler.getCookieAccessFilter(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rr));
        if (handler == null) return INVALID_PERSISTENT;

        boolean isPersistent = handler instanceof PersistentHandler;
        if (!isPersistent) {
            CefLog.Error("Non-persistent CefCookieAccessFilter can cause unstable behaviour and will not be used. Please use PersistentHandler.");
            return INVALID_PERSISTENT;
        }

        RemoteCookieAccessFilter resultHandler = RemoteCookieAccessFilter.create(filter);
        return resultHandler.thriftId(true);
    }

    @Override
    public void ResourceRequestHandler_Dispose(int rrHandler)  {
        RemoteResourceRequestHandler.FACTORY.dispose(rrHandler);
    }

    @Override
    public void CookieAccessFilter_Dispose(int filter)  {
        RemoteCookieAccessFilter.FACTORY.dispose(filter);
    }

    private static CefCookie cookieFromList(List<String> cookie) {
        try {
            return new CefCookie(
                    cookie.get(0),
                    cookie.get(1),
                    cookie.get(2),
                    cookie.get(3),
                    Boolean.parseBoolean(cookie.get(4)),
                    Boolean.parseBoolean(cookie.get(5)),
                    new Date(Long.parseLong(cookie.get(6))),
                    new Date(Long.parseLong(cookie.get(7))),
                    Boolean.parseBoolean(cookie.get(8)),
                    new Date(Long.parseLong(cookie.get(9)))
            );
        } catch (NumberFormatException e) {
            CefLog.Error("Can't parse cookie: err %s, list: '%s'", e.getMessage(), Arrays.toString(cookie.toArray()));
            return null;
        }
    }

    @Override
    public boolean CookieAccessFilter_CanSendCookie(int filter, int bid, RObject request, List<String> cookie)  {
        RemoteCookieAccessFilter f = RemoteCookieAccessFilter.FACTORY.get(filter);
        if (f == null) return false;

        RemoteRequestImpl rr = new RemoteRequestImpl(myServer, request);
        boolean result = f.getDelegate().canSendCookie(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rr), cookieFromList(cookie));
        return result;
    }

    @Override
    public boolean CookieAccessFilter_CanSaveCookie(int filter, int bid, RObject request, RObject response, List<String> cookie)  {
        RemoteCookieAccessFilter f = RemoteCookieAccessFilter.FACTORY.get(filter);
        if (f == null) return false;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myServer, request);
        RemoteResponseImpl rresp = new RemoteResponseImpl(myServer, response);
        boolean result = f.getDelegate().canSaveCookie(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rreq), new RemoteResponse(rresp), cookieFromList(cookie));
        return result;
    }

    @Override
    public boolean RequestHandler_OnOpenURLFromTab(int bid, String target_url, boolean user_gesture) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        return rh.onOpenURLFromTab(browser, NULL_FRAME, target_url, user_gesture);
    }

    @Override
    public boolean RequestHandler_GetAuthCredentials(int bid, String origin_url, boolean isProxy, String host, int port, String realm, String scheme, RObject authCallback) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        CefAuthCallback callback = new RemoteAuthCallback(myServer, authCallback);
        return rh.getAuthCredentials(browser, origin_url, isProxy, host, port, realm, scheme, callback);
    }

    @Override
    public boolean RequestHandler_OnCertificateError(int bid, String cert_error, String request_url, RObject sslInfo, RObject callback) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        CefCallback cb = new RemoteCallback(myServer, callback);
        CefSSLInfo ssl = new RemoteSSLInfo(sslInfo);
        CefLoadHandler.ErrorCode err = CefLoadHandler.ErrorCode.ERR_NONE;
        if (cert_error != null && !cert_error.isEmpty()) {
            try {
                err = CefLoadHandler.ErrorCode.valueOf(cert_error);
            } catch (IllegalArgumentException e) {
                CefLog.Error("OnCertificateError: ", e.getMessage());
            }
        }
        return rh.onCertificateError(browser, err, request_url, ssl, cb);
    }

    @Override
    public void RequestHandler_OnRenderProcessTerminated(int bid, String status) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return;

        CefRequestHandler.TerminationStatus s = CefRequestHandler.TerminationStatus.TS_ABNORMAL_TERMINATION;
        if (status != null && !status.isEmpty()) {
            try {
                s = CefRequestHandler.TerminationStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                CefLog.Error("onRenderProcessTerminated: ", e.getMessage());
            }
        }
        rh.onRenderProcessTerminated(browser, s);
    }

    @Override
    public boolean ResourceRequestHandler_OnBeforeResourceLoad(int rrHandler, int bid, RObject request) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return false;

        RemoteRequestImpl rr = new RemoteRequestImpl(myServer, request);
        boolean result = rrrh.getDelegate().onBeforeResourceLoad(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rr));
        return result;
    }

    @Override
    public RObject ResourceRequestHandler_GetResourceHandler(int rrHandler, int bid, RObject request) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return INVALID_PERSISTENT;

        RemoteRequestImpl rr = new RemoteRequestImpl(myServer, request);
        CefResourceHandler handler = rrrh.getDelegate().getResourceHandler(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rr));
        if (handler == null) return INVALID_PERSISTENT;

        boolean isPersistent = handler instanceof PersistentHandler;
        if (!isPersistent) {
            CefLog.Error("Non-persistent CefResourceHandler can cause unstable behaviour and will not be used. Please use PersistentHandler.");
            return INVALID_PERSISTENT;
        }

        RemoteResourceHandler result = RemoteResourceHandler.create(handler);
        return result.thriftId(true);
    }

    @Override
    public String ResourceRequestHandler_OnResourceRedirect(int rrHandler, int bid, RObject request, RObject response, String new_url) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return "";

        RemoteRequestImpl rreq = new RemoteRequestImpl(myServer, request);
        RemoteResponseImpl rresp = new RemoteResponseImpl(myServer, response);
        StringRef sref = new StringRef(new_url);
        rrrh.getDelegate().onResourceRedirect(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rreq), new RemoteResponse(rresp), sref);
        return sref.get();
    }

    @Override
    public boolean ResourceRequestHandler_OnResourceResponse(int rrHandler, int bid, RObject request, RObject response) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return false;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myServer, request);
        RemoteResponseImpl rresp = new RemoteResponseImpl(myServer, response);
        boolean result = rrrh.getDelegate().onResourceResponse(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rreq), new RemoteResponse(rresp));
        return result;
    }

    @Override
    public void ResourceRequestHandler_OnResourceLoadComplete(int rrHandler, int bid, RObject request, RObject response, String status, long receivedContentLength) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myServer, request);
        RemoteResponseImpl rresp = new RemoteResponseImpl(myServer, response);
        CefURLRequest.Status s = CefURLRequest.Status.UR_UNKNOWN;
        if (status != null && !status.isEmpty()) {
            try {
                s = CefURLRequest.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                CefLog.Error("OnResourceLoadComplete: ", e.getMessage());
            }
        }
        rrrh.getDelegate().onResourceLoadComplete(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rreq), new RemoteResponse(rresp), s, receivedContentLength);
    }

    @Override
    public boolean ResourceRequestHandler_OnProtocolExecution(int rrHandler, int bid, RObject request, boolean allowOsExecution) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return false;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myServer, request);
        BoolRef br = new BoolRef(allowOsExecution);
        rrrh.getDelegate().onProtocolExecution(getRemoteBrowser(bid), NULL_FRAME, new RemoteRequest(rreq), br);
        return br.get();
    }

    @Override
    public void ResourceHandler_Dispose(int resHandler) throws TException {
        RemoteResourceHandler.FACTORY.dispose(resHandler);
    }

    @Override
    public boolean MessageRouterHandler_onQuery(RObject handler, int bid, long queryId, String request, boolean persistent, RObject queryCallback) throws TException {
        RemoteMessageRouterHandler rmrh = RemoteMessageRouterHandler.FACTORY.get(handler.objId);
        if (rmrh == null) return false;

        RemoteQueryCallback rcb = new RemoteQueryCallback(myServer, queryCallback);
        return rmrh.getDelegate().onQuery(getRemoteBrowser(bid), NULL_FRAME, queryId, request, persistent, rcb);
    }

    @Override
    public void MessageRouterHandler_onQueryCanceled(RObject handler, int bid, long queryId) throws TException {
        RemoteMessageRouterHandler rmrh = RemoteMessageRouterHandler.FACTORY.get(handler.objId);
        if (rmrh == null) return;

        rmrh.getDelegate().onQueryCanceled(getRemoteBrowser(bid), NULL_FRAME, queryId);
    }
}
