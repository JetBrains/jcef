package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.callback.RemoteAuthCallback;
import com.jetbrains.cef.remote.callback.RemoteCallback;
import com.jetbrains.cef.remote.callback.RemoteCompletionCallback;
import com.jetbrains.cef.remote.callback.RemoteSchemeHandlerFactory;
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
import org.cef.misc.*;
import org.cef.network.CefCookie;
import org.cef.network.CefRequest;
import org.cef.network.CefURLRequest;
import org.cef.security.CefSSLInfo;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

//
// Service for rpc from native to java
//
public class ClientHandlersImpl implements ClientHandlers.Iface {
    private static final boolean TRACE_REMOTE_FIND_BID = Utils.getBoolean("TRACE_REMOTE_FIND_BID");
    private final Map<Integer, RemoteBrowser> myBid2RemoteBrowser;
    private Runnable myOnContextInitialized;
    private final RpcExecutor myService;

    public ClientHandlersImpl(RpcExecutor service, Map<Integer, RemoteBrowser> bid2RemoteBrowser) {
        myService = service;
        myBid2RemoteBrowser = bid2RemoteBrowser;
    }

    public void setOnContextInitialized(Runnable onContextInitialized) {
        myOnContextInitialized = onContextInitialized;
    }

    private RemoteBrowser getRemoteBrowser(int bid) {
        RemoteBrowser browser = myBid2RemoteBrowser.get(bid);
        if (browser == null) {
            if (TRACE_REMOTE_FIND_BID) CefLog.Debug("Can't find remote browser with bid=%d.", bid);
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
        // Called on the server process UI thread immediately after the CEF context
        // has been initialized.
        CefLog.Debug("AppHandler_OnContextInitialized: ");
        if (myOnContextInitialized != null)
            myOnContextInitialized.run();
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
        CefRenderHandler rh = browser.getRenderHandler();
        if (rh == null) return INVALID_RECT;
        Rectangle rect = rh.getViewRect(browser);
        return new Rect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public ScreenInfo RenderHandler_GetScreenInfo(int bid) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return INVALID_SCREENINFO;
        CefRenderHandler rh = browser.getRenderHandler();
        if (rh == null) return INVALID_SCREENINFO;
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
        CefRenderHandler rh = browser.getRenderHandler();
        if (rh == null) return INVALID_POINT;
        java.awt.Point res = rh.getScreenPoint(browser, new java.awt.Point(viewX, viewY));
        return new Point(res.x, res.y);
    }

    @Override
    public void RenderHandler_OnPaint(int bid, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, int width, int height) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        CefRenderHandler rh = browser.getRenderHandler();
        if (rh == null) return;
        ((CefNativeRenderHandler)rh).onPaintWithSharedMem(browser, popup, dirtyRectsCount, sharedMemName, sharedMemHandle, width, height);
    }

    //
    // CefLifeSpanHandler
    //

    @Override
    public boolean LifeSpanHandler_OnBeforePopup(int bid, RObject frame, String url, String frameName, boolean gesture) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;

        RemoteFrame rframe = new RemoteFrame(myService, frame);
        return browser.getOwner().hLifeSpan.handleBool(lsh-> lsh.onBeforePopup(browser, rframe, url, frameName));
    }

    @Override
    public void LifeSpanHandler_OnAfterCreated(int bid, int nativeBrowserIdentifier) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        browser.getOwner().onAfterCreated(browser, nativeBrowserIdentifier);
    }

    @Override
    public void LifeSpanHandler_OnBeforeClose(int bid) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        browser.getOwner().onBeforeClosed(browser);
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

    //
    // CefLoadHandler
    //
    @Override
    public void LoadHandler_OnLoadingStateChange(int bid, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        CefLoadHandler loadHandler = browser.getOwner().getLoadHandler();
        if (loadHandler == null) return;

        loadHandler.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
    }

    @Override
    public void LoadHandler_OnLoadStart(int bid, RObject frame, int transition_type) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        CefLoadHandler loadHandler = browser.getOwner().getLoadHandler();
        if (loadHandler == null) return;

        // TODO: use correct transition_type instead of TT_LINK
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        loadHandler.onLoadStart(browser, rframe, CefRequest.TransitionType.TT_LINK);
    }

    @Override
    public void LoadHandler_OnLoadEnd(int bid, RObject frame, int httpStatusCode) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        CefLoadHandler loadHandler = browser.getOwner().getLoadHandler();
        if (loadHandler == null) return;

        RemoteFrame rframe = new RemoteFrame(myService, frame);
        loadHandler.onLoadEnd(browser, rframe, httpStatusCode);
    }

    @Override
    public void LoadHandler_OnLoadError(int bid, RObject frame, int errorCode, String errorText, String failedUrl) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        CefLoadHandler loadHandler = browser.getOwner().getLoadHandler();
        if (loadHandler == null) return;

        RemoteFrame rframe = new RemoteFrame(myService, frame);
        loadHandler.onLoadError(browser, rframe, CefLoadHandler.ErrorCode.findByCode(errorCode), errorText, failedUrl);
    }

    //
    // CefDisplayHandler
    //

    @Override
    public void DisplayHandler_OnAddressChange(int bid, RObject frame, String url) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        CefDisplayHandler dh = browser.getOwner().getDisplayHandler();
        if (dh == null) return;

        RemoteFrame rframe = new RemoteFrame(myService, frame);
        dh.onAddressChange(browser, rframe, url);
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
    // CefKeyboardHandler (all methods will be called on the UI thread).
    //

    @Override
    public boolean KeyboardHandler_OnPreKeyEvent(int bid, KeyEvent event) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;
        CefKeyboardHandler kh = browser.getOwner().getKeyboardHandler();
        if (kh == null) return false;

        return kh.onPreKeyEvent(browser, thrift2jcef(event), new BoolRef());
    }

    private static CefKeyboardHandler.CefKeyEvent thrift2jcef(KeyEvent event) {
        if (event == null)
            return null;
        if (event.type == null || event.type.isEmpty()) {
            CefLog.Error("Empty key event type: ");
            return null;
        }

        CefKeyboardHandler.CefKeyEvent.EventType type;
        try {
            type = CefKeyboardHandler.CefKeyEvent.EventType.valueOf(event.type);
        } catch (IllegalArgumentException e) {
            CefLog.Error("Unknown key event type: ", e.getMessage());
            return null;
        }

        return new CefKeyboardHandler.CefKeyEvent(type, event.modifiers, event.windows_key_code, event.native_key_code, event.is_system_key, (char)event.character, (char)event.unmodified_character, event.focus_on_editable_field);
    }

    @Override
    public boolean KeyboardHandler_OnKeyEvent(int bid, KeyEvent event) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;
        CefKeyboardHandler kh = browser.getOwner().getKeyboardHandler();
        if (kh == null) return false;

        return kh.onKeyEvent(browser, thrift2jcef(event));
    }

    //
    // CefFocusHandler (all methods will be called on the UI thread).
    //

    @Override
    public void FocusHandler_OnTakeFocus(int bid, boolean next) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        CefFocusHandler fh = browser.getOwner().getFocusHandler();
        if (fh == null) return;

        fh.onTakeFocus(browser, next);
    }

    @Override
    public boolean FocusHandler_OnSetFocus(int bid, String source) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;
        CefFocusHandler fh = browser.getOwner().getFocusHandler();
        if (fh == null) return false;

        CefFocusHandler.FocusSource src = CefFocusHandler.FocusSource.FOCUS_SOURCE_NAVIGATION;
        if (source != null && !source.isEmpty()) {
            try {
                src = CefFocusHandler.FocusSource.valueOf(source);
            } catch (IllegalArgumentException e) {
                CefLog.Error("FocusHandler_OnSetFocus: ", e.getMessage());
            }
        }

        return fh.onSetFocus(browser, src);
    }

    @Override
    public void FocusHandler_OnGotFocus(int bid) throws TException {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;
        CefFocusHandler fh = browser.getOwner().getFocusHandler();
        if (fh == null) return;

        fh.onGotFocus(browser);
    }

    //
    // CefRequestHandler
    //

    ///
    /// Called on the UI thread before browser navigation. Return true to cancel
    /// the navigation or false to allow the navigation to proceed. The |request|
    /// object cannot be modified in this callback.
    /// CefLoadHandler::OnLoadingStateChange will be called twice in all cases.
    /// If the navigation is allowed CefLoadHandler::OnLoadStart and
    /// CefLoadHandler::OnLoadEnd will be called. If the navigation is canceled
    /// CefLoadHandler::OnLoadError will be called with an |errorCode| value of
    /// ERR_ABORTED. The |user_gesture| value will be true if the browser
    /// navigated via explicit user gesture (e.g. clicking a link) or false if it
    /// navigated automatically (e.g. via the DomContentLoaded event).
    ///
    /*--cef()--*/
    @Override
    public boolean RequestHandler_OnBeforeBrowse(int bid, RObject frame, RObject request, boolean user_gesture, boolean is_redirect) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;
        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        RemoteRequestImpl rr = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        return rh.onBeforeBrowse(browser, rframe, new RemoteRequest(rr), user_gesture, is_redirect);
    }

    private static final RObject INVALID = new RObject(-1);

    ///
    /// Called on the browser process IO thread before a resource request is
    /// initiated. The |browser| and |frame| values represent the source of the
    /// request. |request| represents the request contents and cannot be modified
    /// in this callback. |is_navigation| will be true if the resource request is
    /// a navigation. |is_download| will be true if the resource request is a
    /// download. |request_initiator| is the origin (scheme + domain) of the page
    /// that initiated the request. Set |disable_default_handling| to true to
    /// disable default handling of the request, in which case it will need to be
    /// handled via CefResourceRequestHandler::GetResourceHandler or it will be
    /// canceled. To allow the resource load to proceed with default handling
    /// return NULL. To specify a handler for the resource return a
    /// CefResourceRequestHandler object. If this callback returns NULL the same
    /// method will be called on the associated CefRequestContextHandler, if any.
    ///
    @Override
    public RObject RequestHandler_GetResourceRequestHandler(int bid, RObject frame, RObject request, boolean isNavigation, boolean isDownload, String requestInitiator) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return INVALID;
        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return INVALID;

        RemoteRequestImpl rr = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        BoolRef disableDefaultHandling = new BoolRef(false);
        CefResourceRequestHandler handler = rh.getResourceRequestHandler(browser, rframe, new RemoteRequest(rr), isNavigation, isDownload, requestInitiator, disableDefaultHandling);
        if (handler == null) return INVALID;

        RemoteResourceRequestHandler resultHandler = RemoteResourceRequestHandler.create(handler);
        return resultHandler.thriftId(disableDefaultHandling.get() ? 1 : 0);
    }

    ///
    /// Called on the IO thread before a resource request is loaded. The |browser|
    /// and |frame| values represent the source of the request, and may be NULL
    /// for requests originating from service workers or CefURLRequest. To
    /// optionally filter cookies for the request return a CefCookieAccessFilter
    /// object. The |request| object cannot not be modified in this callback.
    ///
    /*--cef(optional_param=browser,optional_param=frame)--*/
    @Override
    public RObject ResourceRequestHandler_GetCookieAccessFilter(int rrHandler, int bid, RObject frame, RObject request) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return INVALID;

        RemoteRequestImpl rr = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        CefCookieAccessFilter filter = rrrh.getDelegate().getCookieAccessFilter(getRemoteBrowser(bid), rframe, new RemoteRequest(rr));
        RemoteCookieAccessFilter resultHandler = RemoteCookieAccessFilter.create(filter);
        return resultHandler.thriftId();
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

    ///
    /// Called on the IO thread before a resource request is sent. The |browser|
    /// and |frame| values represent the source of the request, and may be NULL
    /// for requests originating from service workers or CefURLRequest. |request|
    /// cannot be modified in this callback. Return true if the specified cookie
    /// can be sent with the request or false otherwise.
    ///
    /*--cef(optional_param=browser,optional_param=frame)--*/
    @Override
    public boolean CookieAccessFilter_CanSendCookie(int filter, int bid, RObject frame, RObject request, List<String> cookie)  {
        RemoteCookieAccessFilter f = RemoteCookieAccessFilter.FACTORY.get(filter);
        if (f == null) return false;

        RemoteRequestImpl rr = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        boolean result = f.getDelegate().canSendCookie(getRemoteBrowser(bid), rframe, new RemoteRequest(rr), cookieFromList(cookie));
        return result;
    }

    ///
    /// Called on the IO thread after a resource response is received. The
    /// |browser| and |frame| values represent the source of the request, and may
    /// be NULL for requests originating from service workers or CefURLRequest.
    /// |request| cannot be modified in this callback. Return true if the
    /// specified cookie returned with the response can be saved or false
    /// otherwise.
    ///
    @Override
    public boolean CookieAccessFilter_CanSaveCookie(int filter, int bid, RObject frame, RObject request, RObject response, List<String> cookie)  {
        RemoteCookieAccessFilter f = RemoteCookieAccessFilter.FACTORY.get(filter);
        if (f == null) return false;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myService, request);
        RemoteResponseImpl rresp = new RemoteResponseImpl(myService, response);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        boolean result = f.getDelegate().canSaveCookie(getRemoteBrowser(bid), rframe, new RemoteRequest(rreq), new RemoteResponse(rresp), cookieFromList(cookie));
        // NOTE: doc doesn't say that response can't be modifed, but call rresp.flush() triggers cooresponding
        // error on server (i.e. resp is immutable) so don't do that.
        return result;
    }

    @Override
    public boolean RequestHandler_OnOpenURLFromTab(int bid, RObject frame, String target_url, boolean user_gesture) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;
        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        RemoteFrame rframe = new RemoteFrame(myService, frame);
        return rh.onOpenURLFromTab(browser, rframe, target_url, user_gesture);
    }

    @Override
    public boolean RequestHandler_GetAuthCredentials(int bid, String origin_url, boolean isProxy, String host, int port, String realm, String scheme, RObject authCallback) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;
        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        CefAuthCallback callback = new RemoteAuthCallback(myService, authCallback);
        return rh.getAuthCredentials(browser, origin_url, isProxy, host, port, realm, scheme, callback);
    }

    @Override
    public boolean RequestHandler_OnCertificateError(int bid, String cert_error, String request_url, ByteBuffer sslInfo, RObject callback) {
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return false;
        CefRequestHandler rh = browser.getOwner().getRequestHandler();
        if (rh == null) return false;

        CefCallback cb = new RemoteCallback(myService, callback);
        CefSSLInfo ssl = RemoteSSLInfo.fromBinary(sslInfo);
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

    ///
    /// Called on the IO thread before a resource request is loaded. The |browser|
    /// and |frame| values represent the source of the request, and may be NULL
    /// for requests originating from service workers or CefURLRequest. To
    /// redirect or change the resource load optionally modify |request|.
    /// Modification of the request URL will be treated as a redirect. Return
    /// RV_CONTINUE to continue the request immediately. Return RV_CONTINUE_ASYNC
    /// and call CefCallback methods at a later time to continue or cancel the
    /// request asynchronously. Return RV_CANCEL to cancel the request
    /// immediately.
    ///
    /*--cef(optional_param=browser,optional_param=frame, default_retval=RV_CONTINUE)--*/
    @Override
    public boolean ResourceRequestHandler_OnBeforeResourceLoad(int rrHandler, int bid, RObject frame, RObject request) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return false;

        RemoteRequestImpl rr = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        boolean result = rrrh.getDelegate().onBeforeResourceLoad(getRemoteBrowser(bid), rframe, new RemoteRequest(rr));
        rr.flush();
        return result;
    }

    ///
    /// Called on the IO thread before a resource is loaded. The |browser| and
    /// |frame| values represent the source of the request, and may be NULL for
    /// requests originating from service workers or CefURLRequest. To allow the
    /// resource to load using the default network loader return NULL. To specify
    /// a handler for the resource return a CefResourceHandler object. The
    /// |request| object cannot not be modified in this callback.
    ///
    /*--cef(optional_param=browser,optional_param=frame)--*/
    @Override
    public RObject ResourceRequestHandler_GetResourceHandler(int rrHandler, int bid, RObject frame, RObject request) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return INVALID;

        RemoteRequestImpl rr = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        CefResourceHandler handler = rrrh.getDelegate().getResourceHandler(getRemoteBrowser(bid), rframe, new RemoteRequest(rr));
        if (handler == null) return INVALID;

        RemoteResourceHandler result = RemoteResourceHandler.create(handler);
        return result.thriftId();
    }

    ///
    /// Begin processing the request. To handle the request return true and call
    /// CefCallback::Continue() once the response header information is available
    /// (CefCallback::Continue() can also be called from inside this method if
    /// header information is available immediately). To cancel the request return
    /// false.
    ///
    /// WARNING: This method is deprecated. Use Open instead.
    ///
    @Override
    public boolean ResourceHandler_ProcessRequest(int resourceHandler, RObject request, RObject callback) throws TException {
        RemoteResourceHandler rrh = RemoteResourceHandler.FACTORY.find(resourceHandler);
        if (rrh == null) return false;

        RemoteRequestImpl rr = new RemoteRequestImpl(myService, request);
        CefCallback cb = new RemoteCallback(myService, callback);
        boolean result = rrh.getDelegate().processRequest(new RemoteRequest(rr), cb);
        rr.flush();
        return result;
     }

    ///
    /// Retrieve response header information. If the response length is not known
    /// set |response_length| to -1 and ReadResponse() will be called until it
    /// returns false. If the response length is known set |response_length|
    /// to a positive value and ReadResponse() will be called until it returns
    /// false or the specified number of bytes have been read. Use the |response|
    /// object to set the mime type, http status code and other optional header
    /// values. To redirect the request to a new URL set |redirectUrl| to the new
    /// URL. |redirectUrl| can be either a relative or fully qualified URL.
    /// It is also possible to set |response| to a redirect http status code
    /// and pass the new URL via a Location header. Likewise with |redirectUrl| it
    /// is valid to set a relative or fully qualified URL as the Location header
    /// value. If an error occured while setting up the request you can call
    /// SetError() on |response| to indicate the error condition.
    ///
    @Override
    public ResponseHeaders ResourceHandler_GetResponseHeaders(int resourceHandler, RObject response) throws TException {
        RemoteResourceHandler rrh = RemoteResourceHandler.FACTORY.find(resourceHandler);
        if (rrh == null) return null;

        RemoteResponseImpl rr = new RemoteResponseImpl(myService, response);
        IntRef respLen = new IntRef();
        StringRef redirectUrlRef = new StringRef();
        rrh.getDelegate().getResponseHeaders(new RemoteResponse(rr), respLen, redirectUrlRef);
        rr.flush();
        ResponseHeaders result = new ResponseHeaders();
        result.setLength(respLen.get());
        if (redirectUrlRef.get() != null)
            result.setRedirectUrl(redirectUrlRef.get());
        return result;
    }

    /**
     * Read response data. If data is available immediately copy up to |bytesToRead| bytes into
     * |dataOut|, set |bytesRead| to the number of bytes copied, and return true. To read the data
     * at a later time set |bytesRead| to 0, return true and call CefCallback.Continue() when the
     * data is available. To indicate response completion return false.
     * @return True if more data is or will be available.
     */
    @Override
    public ResponseData ResourceHandler_ReadResponse(int resourceHandler, int bytes_to_read, RObject callback) throws TException {
        if (bytes_to_read <= 0) return new ResponseData();
        if (bytes_to_read > 256*1024)
            CefLog.Error("ResourceHandler_ReadResponse: too much bytes to read %d. Need to implement via shared memory.", bytes_to_read);

        RemoteResourceHandler rrh = RemoteResourceHandler.FACTORY.find(resourceHandler);
        if (rrh == null) return null;

        CefCallback cb = new RemoteCallback(myService, callback);

        byte[] buf = new byte[bytes_to_read];
        IntRef bytesRead = new IntRef();
        final boolean continueRead = rrh.getDelegate().readResponse(buf, bytes_to_read, bytesRead, cb);
        final int read = bytesRead.get();
        ResponseData result = new ResponseData();
        result.setContinueRead(continueRead);
        result.setBytes_read(read);
        result.setData(ByteBuffer.wrap(buf, 0, read));
        return result;
    }

    @Override
    public void ResourceHandler_Cancel(int resourceHandler) throws TException {
        RemoteResourceHandler rrh = RemoteResourceHandler.FACTORY.find(resourceHandler);
        if (rrh == null) return;

        rrh.getDelegate().cancel();
    }

    ///
    /// Called on the IO thread when a resource load is redirected. The |browser|
    /// and |frame| values represent the source of the request, and may be NULL
    /// for requests originating from service workers or CefURLRequest. The
    /// |request| parameter will contain the old URL and other request-related
    /// information. The |response| parameter will contain the response that
    /// resulted in the redirect. The |new_url| parameter will contain the new URL
    /// and can be changed if desired. The |request| and |response| objects cannot
    /// be modified in this callback.
    ///
    /*--cef(optional_param=browser,optional_param=frame)--*/
    @Override
    public String ResourceRequestHandler_OnResourceRedirect(int rrHandler, int bid, RObject frame, RObject request, RObject response, String new_url) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return "";

        RemoteRequestImpl rreq = new RemoteRequestImpl(myService, request);
        RemoteResponseImpl rresp = new RemoteResponseImpl(myService, response);
        StringRef sref = new StringRef(new_url);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        rrrh.getDelegate().onResourceRedirect(getRemoteBrowser(bid), rframe, new RemoteRequest(rreq), new RemoteResponse(rresp), sref);
        return sref.get();
    }

    ///
    /// Called on the IO thread when a resource response is received. The
    /// |browser| and |frame| values represent the source of the request, and may
    /// be NULL for requests originating from service workers or CefURLRequest. To
    /// allow the resource load to proceed without modification return false. To
    /// redirect or retry the resource load optionally modify |request| and return
    /// true. Modification of the request URL will be treated as a redirect.
    /// Requests handled using the default network loader cannot be redirected in
    /// this callback. The |response| object cannot be modified in this callback.
    ///
    /// WARNING: Redirecting using this method is deprecated. Use
    /// OnBeforeResourceLoad or GetResourceHandler to perform redirects.
    ///
    /*--cef(optional_param=browser,optional_param=frame)--*/
    @Override
    public boolean ResourceRequestHandler_OnResourceResponse(int rrHandler, int bid, RObject frame, RObject request, RObject response) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return false;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myService, request);
        RemoteResponseImpl rresp = new RemoteResponseImpl(myService, response);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        boolean result = rrrh.getDelegate().onResourceResponse(getRemoteBrowser(bid), rframe, new RemoteRequest(rreq), new RemoteResponse(rresp));
        rreq.flush(); // |response| object cannot be modified in this callback.
        return result;
    }

    ///
    /// Called on the IO thread when a resource load has completed. The |browser|
    /// and |frame| values represent the source of the request, and may be NULL
    /// for requests originating from service workers or CefURLRequest. |request|
    /// and |response| represent the request and response respectively and cannot
    /// be modified in this callback. |status| indicates the load completion
    /// status. |received_content_length| is the number of response bytes actually
    /// read. This method will be called for all requests, including requests that
    /// are aborted due to CEF shutdown or destruction of the associated browser.
    /// In cases where the associated browser is destroyed this callback may
    /// arrive after the CefLifeSpanHandler::OnBeforeClose callback for that
    /// browser. The CefFrame::IsValid method can be used to test for this
    /// situation, and care should be taken not to call |browser| or |frame|
    /// methods that modify state (like LoadURL, SendProcessMessage, etc.) if the
    /// frame is invalid.
    ///
    /*--cef(optional_param=browser,optional_param=frame)--*/
    @Override
    public void ResourceRequestHandler_OnResourceLoadComplete(int rrHandler, int bid, RObject frame, RObject request, RObject response, String status, long receivedContentLength) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myService, request);
        RemoteResponseImpl rresp = new RemoteResponseImpl(myService, response);
        CefURLRequest.Status s = CefURLRequest.Status.UR_UNKNOWN;
        if (status != null && !status.isEmpty()) {
            try {
                s = CefURLRequest.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                CefLog.Error("OnResourceLoadComplete: ", e.getMessage());
            }
        }
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        rrrh.getDelegate().onResourceLoadComplete(getRemoteBrowser(bid), rframe, new RemoteRequest(rreq), new RemoteResponse(rresp), s, receivedContentLength);
    }

    ///
    /// Called on the IO thread to handle requests for URLs with an unknown
    /// protocol component. The |browser| and |frame| values represent the source
    /// of the request, and may be NULL for requests originating from service
    /// workers or CefURLRequest. |request| cannot be modified in this callback.
    /// Set |allow_os_execution| to true to attempt execution via the registered
    /// OS protocol handler, if any. SECURITY WARNING: YOU SHOULD USE THIS METHOD
    /// TO ENFORCE RESTRICTIONS BASED ON SCHEME, HOST OR OTHER URL ANALYSIS BEFORE
    /// ALLOWING OS EXECUTION.
    ///
    /*--cef(optional_param=browser,optional_param=frame)--*/
    @Override
    public boolean ResourceRequestHandler_OnProtocolExecution(int rrHandler, int bid, RObject frame, RObject request, boolean allowOsExecution) {
        RemoteResourceRequestHandler rrrh = RemoteResourceRequestHandler.FACTORY.get(rrHandler);
        if (rrrh == null) return false;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        BoolRef br = new BoolRef(allowOsExecution);
        rrrh.getDelegate().onProtocolExecution(getRemoteBrowser(bid), rframe, new RemoteRequest(rreq), br);
        return br.get();
    }

    @Override
    public void ResourceHandler_Dispose(int resHandler) throws TException {
        RemoteResourceHandler.FACTORY.dispose(resHandler);
    }

    @Override
    public boolean MessageRouterHandler_onQuery(RObject handler, int bid, RObject frame, long queryId, String request, boolean persistent, RObject queryCallback) throws TException {
        RemoteMessageRouterHandler rmrh = RemoteMessageRouterHandler.FACTORY.get(handler.objId);
        if (rmrh == null) return false;

        RemoteQueryCallback rcb = new RemoteQueryCallback(myService, queryCallback);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        return rmrh.getDelegate().onQuery(getRemoteBrowser(bid), rframe, queryId, request, persistent, rcb);
    }

    @Override
    public void MessageRouterHandler_onQueryCanceled(RObject handler, int bid, RObject frame, long queryId) throws TException {
        RemoteMessageRouterHandler rmrh = RemoteMessageRouterHandler.FACTORY.get(handler.objId);
        if (rmrh == null) return;

        RemoteFrame rframe = new RemoteFrame(myService, frame);
        rmrh.getDelegate().onQueryCanceled(getRemoteBrowser(bid), rframe, queryId);
    }

    @Override
    public void MessageRouterHandler_Dispose(int handler) throws TException {
        RemoteMessageRouterHandler.FACTORY.dispose(handler);
    }

    @Override
    public RObject SchemeHandlerFactory_CreateHandler(int schemeHandlerFactory, int bid, RObject frame, String scheme_name, RObject request) throws TException {
        RemoteSchemeHandlerFactory sf = RemoteSchemeHandlerFactory.FACTORY.get(schemeHandlerFactory);
        if (sf == null) return INVALID;

        RemoteRequestImpl rreq = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        CefResourceHandler handler = sf.getDelegate().create(getRemoteBrowser(bid), rframe, scheme_name, new RemoteRequest(rreq));
        if (handler == null) return INVALID;

        RemoteResourceHandler result = RemoteResourceHandler.create(handler);
        return result.thriftId();
    }

    @Override
    public void SchemeHandlerFactory_Dispose(int schemeHandlerFactory) throws TException {
        RemoteSchemeHandlerFactory.FACTORY.dispose(schemeHandlerFactory);
    }

    @Override
    public void CompletionCallback_OnComplete(int completionCallback) throws TException {
        RemoteCompletionCallback cc = RemoteCompletionCallback.FACTORY.get(completionCallback);
        if (cc == null) return;

        cc.getDelegate().onComplete();
        RemoteCompletionCallback.FACTORY.dispose(completionCallback);
    }

    @Override
    public RObject RequestContextHandler_GetResourceRequestHandler(int handlerId, int bid, RObject frame, RObject request, boolean isNavigation, boolean isDownload, String requestInitiator) throws TException {
        RemoteRequestContextHandler rhandler = RemoteRequestContextHandler.FACTORY.get(handlerId);
        RemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null || rhandler == null) return INVALID;

        RemoteRequestImpl rr = new RemoteRequestImpl(myService, request);
        RemoteFrame rframe = new RemoteFrame(myService, frame);
        BoolRef disableDefaultHandling = new BoolRef(false);
        CefResourceRequestHandler handler = rhandler.getDelegate().getResourceRequestHandler(browser, rframe, new RemoteRequest(rr), isNavigation, isDownload, requestInitiator, disableDefaultHandling);
        if (handler == null) return INVALID;

        RemoteResourceRequestHandler resultHandler = RemoteResourceRequestHandler.create(handler);
        return resultHandler.thriftId(disableDefaultHandling.get() ? 1 : 0);
    }

    @Override
    public boolean CookieVisitor_Visit(int visitor, Cookie cookie, int count, int total) throws TException {
        RemoteCookieVisitor rvisitor = RemoteCookieVisitor.FACTORY.get(visitor);
        if (rvisitor == null) return false;

        BoolRef delete = new BoolRef(false);
        boolean continueTraverse = rvisitor.getDelegate().visit(RemoteCookieManager.toCefCookie(cookie), count, total, delete);
        if (delete.get())
            CefLog.Error("Can't delete cookie %s via CefCookieVisitor, please use CefCookieManager.deleteCookie. TODO: implement.");
        if (count == total || !continueTraverse) {
            CefLog.Debug("Last cookie (%d) visited, dispose RemoteCookieVisitor %d.", total, visitor);
            RemoteCookieVisitor.FACTORY.dispose(visitor);
        }
        return continueTraverse;
    }

    @Override
    public void CookieVisitor_Dispose(int visitor) throws TException {
        CefLog.Debug("Dispose RemoteCookieVisitor %d (by server request).", visitor);
        RemoteCookieVisitor.FACTORY.dispose(visitor);
    }
}
