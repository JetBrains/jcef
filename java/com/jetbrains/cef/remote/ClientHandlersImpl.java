package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.callback.RemoteAuthCallback;
import com.jetbrains.cef.remote.callback.RemoteCallback;
import com.jetbrains.cef.remote.network.*;
import com.jetbrains.cef.remote.router.RemoteMessageRouterHandler;
import com.jetbrains.cef.remote.router.RemoteQueryCallback;
import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import com.jetbrains.cef.remote.thrift_codegen.CustomScheme;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.apache.thrift.TException;
import org.cef.CefSettings;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//
// Service for rpc from native to java
//
public class ClientHandlersImpl implements ClientHandlers.Iface {
    private final Map<Integer, RemoteBrowser> myBid2RemoteBrowser = new ConcurrentHashMap<>();
    private final RemoteApp myRemoteApp;
    private final RpcExecutor myServer;

    public ClientHandlersImpl(RpcExecutor server, RemoteApp remoteApp) {
        myRemoteApp = remoteApp;
        myServer = server;
    }

    void registerBrowser(RemoteBrowser browser) {
        myBid2RemoteBrowser.put(browser.getBid(), browser);
    }

    public void unregisterBrowser(int bid) {
        RemoteBrowser browser = myBid2RemoteBrowser.remove(bid);
        if (browser == null) {
            CefLog.Error("unregisterBrowser: bid=%d was already removed.");
            return;
        }
        browser.getOwner().disposeClient();
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
    public void onContextInitialized() {
        CefLog.Debug("onContextInitialized: ");
        myRemoteApp.onContextInitialized();
    }

    @Override
    public List<CustomScheme> getRegisteredCustomSchemes() {
        CefLog.Debug("onRegisterCustomSchemes: ");
        return myRemoteApp.getAllRegisteredCustomSchemes();
    }

    //
    // CefRenderHandler
    //

    private static final ByteBuffer ZERO_BUFFER = ByteBuffer.allocate(4).putInt(0);

    @Override
    public ByteBuffer getInfo(int bid, String request, ByteBuffer buffer) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return ZERO_BUFFER;

        RemoteClient rclient = browser.getOwner();
        CefRenderHandler rh = rclient.getRenderHandler();
        if (rh == null) return ZERO_BUFFER;

        request = request == null ? "" : request.toLowerCase();
        int[] data = new int[0];
        if ("viewrect".equals(request)) {
            Rectangle rect = rh.getViewRect(browser);
            data = new int[]{rect.x, rect.y, rect.width, rect.height};
        } else if ("screeninfo".equals(request)) {
            CefScreenInfo csi = new CefScreenInfo();
            boolean success = rh.getScreenInfo(browser, csi);
            if (success) {
                data = new int[]{
                        (int) csi.device_scale_factor,
                        csi.depth,
                        csi.depth_per_component,
                        csi.is_monochrome ? 1 : 0,
                        csi.x,
                        csi.y,
                        csi.width,
                        csi.height,
                        csi.available_x,
                        csi.available_y,
                        csi.available_width,
                        csi.available_height
                };
            } else {
                data = new int[]{0};
            }
        } else if ("screenpoint".equals(request)) {
            Point pt = new Point(buffer.getInt(), buffer.getInt());
            Point res = rh.getScreenPoint(browser, pt);
            data = new int[]{res.x, res.y};
        } else {
            CefLog.Error("getInfo, unknown request: " + request);
            return ZERO_BUFFER;
        }

        ByteBuffer result = ByteBuffer.allocate(data.length*4);
        result.order(ByteOrder.nativeOrder());
        result.asIntBuffer().put(data);
        return result;
    }

    @Override
    public void onPaint(int bid, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, boolean recreateHandle, int width, int height) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        RemoteClient rc = browser.getOwner();
        CefRenderHandler rh = rc.getRenderHandler();
        if (rh == null) return;

        ((CefNativeRenderHandler)rh).onPaintWithSharedMem(browser, popup, dirtyRectsCount, sharedMemName, sharedMemHandle, recreateHandle, width, height);
    }

    //
    // CefLifeSpanHandler
    //

    @Override
    public void onBeforePopup(int bid, String url, String frameName, boolean gesture) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLifeSpanHandler lsh = browser.getOwner().getLifeSpanHandler();
        if (lsh == null) return;

        lsh.onBeforePopup(browser, null, url, frameName);
    }

    @Override
    public void onAfterCreated(int bid) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLifeSpanHandler lsh = browser.getOwner().getLifeSpanHandler();
        if (lsh == null) return;

        lsh.onAfterCreated(browser);
    }

    @Override
    public void doClose(int bid) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLifeSpanHandler lsh = browser.getOwner().getLifeSpanHandler();
        if (lsh == null) return;

        lsh.doClose(browser);
    }

    @Override
    public void onBeforeClose(int bid) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLifeSpanHandler lsh = browser.getOwner().getLifeSpanHandler();
        if (lsh == null) return;

        lsh.onBeforeClose(browser);
    }


    //
    // CefLoadHandler
    //

    @Override
    public void onLoadingStateChange(int bid, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLoadHandler lh = browser.getOwner().getLoadHandler();
        if (lh == null) return;

        lh.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
    }

    @Override
    public void onLoadStart(int bid, int transition_type) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLoadHandler lh = browser.getOwner().getLoadHandler();
        if (lh == null) return;

        // TODO: use correct transition_type instead of TT_LINK
        lh.onLoadStart(browser, null, CefRequest.TransitionType.TT_LINK);
    }

    @Override
    public void onLoadEnd(int bid, int httpStatusCode) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLoadHandler lh = browser.getOwner().getLoadHandler();
        if (lh == null) return;

        lh.onLoadEnd(browser, null, httpStatusCode);
    }

    @Override
    public void onLoadError(int bid, int errorCode, String errorText, String failedUrl) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLoadHandler lh = browser.getOwner().getLoadHandler();
        if (lh == null) return;

        lh.onLoadError(browser, null, CefLoadHandler.ErrorCode.findByCode(errorCode), errorText, failedUrl);
    }

    //
    // CefDisplayHandler
    //

    @Override
    public void onAddressChange(int bid, String url) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return;

        dh.onAddressChange(browser, null, url);
    }

    @Override
    public void onTitleChange(int bid, String title) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return;

        dh.onTitleChange(browser, title);
    }

    @Override
    public boolean onTooltip(int bid, String text) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return false;

        return dh.onTooltip(browser, text);
    }

    @Override
    public void onStatusMessage(int bid, String value) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return;

        dh.onStatusMessage(browser, value);
    }

    @Override
    public boolean onConsoleMessage(int bid, int level, String message, String source, int line) {
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

        RemoteRequest rr = new RemoteRequest(myServer, request);
        boolean result = rh.onBeforeBrowse(browser, null, rr, user_gesture, is_redirect);
        rr.flush();
        return result;
    }

    private static final RObject INVALID_PERSISTENT = new RObject(-1).setIsPersistent(true);

    @Override
    public RObject RequestHandler_GetResourceRequestHandler(int bid, RObject request, boolean isNavigation, boolean isDownload, String requestInitiator) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return INVALID_PERSISTENT;

        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return INVALID_PERSISTENT;

        RemoteRequest rr = new RemoteRequest(myServer, request);
        BoolRef disableDefaultHandling = new BoolRef(false);
        CefResourceRequestHandler handler = rh.getResourceRequestHandler(browser, null, rr, isNavigation, isDownload, requestInitiator, disableDefaultHandling);
        rr.flush();
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
        RemoteRequest rr = new RemoteRequest(myServer, request);
        CefCookieAccessFilter filter = handler.getCookieAccessFilter(getRemoteBrowser(bid), null, rr);
        rr.flush();
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

        RemoteRequest rr = new RemoteRequest(myServer, request);
        boolean result = f.getDelegate().canSendCookie(getRemoteBrowser(bid), null, rr, cookieFromList(cookie));
        rr.flush();
        return result;
    }

    @Override
    public boolean CookieAccessFilter_CanSaveCookie(int filter, int bid, RObject request, RObject response, List<String> cookie)  {
        RemoteCookieAccessFilter f = RemoteCookieAccessFilter.FACTORY.get(filter);
        if (f == null) return false;

        RemoteRequest rreq = new RemoteRequest(myServer, request);
        RemoteResponse rresp = new RemoteResponse(myServer, response);
        boolean result = f.getDelegate().canSaveCookie(getRemoteBrowser(bid), null, rreq, rresp, cookieFromList(cookie));
        rreq.flush();
        rresp.flush();
        return result;
    }

    @Override
    public boolean RequestHandler_OnOpenURLFromTab(int bid, String target_url, boolean user_gesture) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        return rh.onOpenURLFromTab(browser, null, target_url, user_gesture);
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

        RemoteRequest rr = new RemoteRequest(myServer, request);
        boolean result = rrrh.getDelegate().onBeforeResourceLoad(getRemoteBrowser(bid), null, rr);
        rr.flush();
        return result;
    }

    @Override
    public RObject ResourceRequestHandler_GetResourceHandler(int rrHandler, int bid, RObject request) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return INVALID_PERSISTENT;

        RemoteRequest rr = new RemoteRequest(myServer, request);
        CefResourceHandler handler = rrrh.getDelegate().getResourceHandler(getRemoteBrowser(bid), null, rr);
        rr.flush();
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

        RemoteRequest rreq = new RemoteRequest(myServer, request);
        RemoteResponse rresp = new RemoteResponse(myServer, response);
        StringRef sref = new StringRef(new_url);
        rrrh.getDelegate().onResourceRedirect(getRemoteBrowser(bid), null, rreq, rresp, sref);
        rreq.flush();
        rresp.flush();
        return sref.get();
    }

    @Override
    public boolean ResourceRequestHandler_OnResourceResponse(int rrHandler, int bid, RObject request, RObject response) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return false;

        RemoteRequest rreq = new RemoteRequest(myServer, request);
        RemoteResponse rresp = new RemoteResponse(myServer, response);
        boolean result = rrrh.getDelegate().onResourceResponse(getRemoteBrowser(bid), null, rreq, rresp);
        rreq.flush();
        rresp.flush();
        return result;
    }

    @Override
    public void ResourceRequestHandler_OnResourceLoadComplete(int rrHandler, int bid, RObject request, RObject response, String status, long receivedContentLength) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return;

        RemoteRequest rreq = new RemoteRequest(myServer, request);
        RemoteResponse rresp = new RemoteResponse(myServer, response);
        CefURLRequest.Status s = CefURLRequest.Status.UR_UNKNOWN;
        if (status != null && !status.isEmpty()) {
            try {
                s = CefURLRequest.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                CefLog.Error("OnResourceLoadComplete: ", e.getMessage());
            }
        }
        rrrh.getDelegate().onResourceLoadComplete(getRemoteBrowser(bid), null, rreq, rresp, s, receivedContentLength);
        rreq.flush();
        rresp.flush();
    }

    @Override
    public boolean ResourceRequestHandler_OnProtocolExecution(int rrHandler, int bid, RObject request, boolean allowOsExecution) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return false;

        RemoteRequest rreq = new RemoteRequest(myServer, request);
        BoolRef br = new BoolRef(allowOsExecution);
        rrrh.getDelegate().onProtocolExecution(getRemoteBrowser(bid), null, rreq, br);
        rreq.flush();
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
        rmrh.getDelegate().onQuery(getRemoteBrowser(bid), null, queryId, request, persistent, rcb);
        return false;
    }

    @Override
    public void MessageRouterHandler_onQueryCanceled(RObject handler, int bid, long queryId) throws TException {

    }
}
