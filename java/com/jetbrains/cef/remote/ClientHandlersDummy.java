package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.*;
import org.apache.thrift.TException;

import java.nio.ByteBuffer;
import java.util.List;

public class ClientHandlersDummy implements ClientHandlers.Iface{

    @Override
    public int connect() throws TException {
        return 0;
    }

    @Override
    public void log(String msg) throws TException {

    }

    @Override
    public void AppHandler_OnContextInitialized() throws TException {

    }

    @Override
    public Rect RenderHandler_GetViewRect(int bid) throws TException {
        return null;
    }

    @Override
    public ScreenInfo RenderHandler_GetScreenInfo(int bid) throws TException {
        return null;
    }

    @Override
    public Point RenderHandler_GetScreenPoint(int bid, int viewX, int viewY) throws TException {
        return null;
    }

    @Override
    public void RenderHandler_OnPaint(int bid, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, int width, int height) throws TException {

    }

    @Override
    public boolean LifeSpanHandler_OnBeforePopup(int bid, RObject frame, String url, String frameName, boolean gesture) throws TException {
        return false;
    }

    @Override
    public void LifeSpanHandler_OnAfterCreated(int bid, int nativeBrowserIdentifier) throws TException {

    }

    @Override
    public boolean LifeSpanHandler_DoClose(int bid) throws TException {
        return false;
    }

    @Override
    public void LifeSpanHandler_OnBeforeClose(int bid) throws TException {

    }

    @Override
    public void LoadHandler_OnLoadingStateChange(int bid, boolean isLoading, boolean canGoBack, boolean canGoForward) throws TException {

    }

    @Override
    public void LoadHandler_OnLoadStart(int bid, RObject frame, int transition_type) throws TException {

    }

    @Override
    public void LoadHandler_OnLoadEnd(int bid, RObject frame, int httpStatusCode) throws TException {

    }

    @Override
    public void LoadHandler_OnLoadError(int bid, RObject frame, int errorCode, String errorText, String failedUrl) throws TException {

    }

    @Override
    public void DisplayHandler_OnAddressChange(int bid, RObject frame, String url) throws TException {

    }

    @Override
    public void DisplayHandler_OnTitleChange(int bid, String title) throws TException {

    }

    @Override
    public boolean DisplayHandler_OnTooltip(int bid, String text) throws TException {
        return false;
    }

    @Override
    public void DisplayHandler_OnStatusMessage(int bid, String value) throws TException {

    }

    @Override
    public boolean DisplayHandler_OnConsoleMessage(int bid, int level, String message, String source, int line) throws TException {
        return false;
    }

    @Override
    public boolean KeyboardHandler_OnPreKeyEvent(int bid, KeyEvent event) throws TException {
        return false;
    }

    @Override
    public boolean KeyboardHandler_OnKeyEvent(int bid, KeyEvent event) throws TException {
        return false;
    }

    @Override
    public void FocusHandler_OnTakeFocus(int bid, boolean next) throws TException {

    }

    @Override
    public boolean FocusHandler_OnSetFocus(int bid, String source) throws TException {
        return false;
    }

    @Override
    public void FocusHandler_OnGotFocus(int bid) throws TException {

    }

    @Override
    public boolean RequestHandler_OnBeforeBrowse(int bid, RObject frame, RObject request, boolean user_gesture, boolean is_redirect) throws TException {
        return false;
    }

    @Override
    public boolean RequestHandler_OnOpenURLFromTab(int bid, RObject frame, String target_url, boolean user_gesture) throws TException {
        return false;
    }

    @Override
    public boolean RequestHandler_GetAuthCredentials(int bid, String origin_url, boolean isProxy, String host, int port, String realm, String scheme, RObject authCallback) throws TException {
        return false;
    }

    @Override
    public boolean RequestHandler_OnCertificateError(int bid, String cert_error, String request_url, ByteBuffer sslInfo, RObject callback) throws TException {
        return false;
    }

    @Override
    public void RequestHandler_OnRenderProcessTerminated(int bid, String status) throws TException {

    }

    @Override
    public RObject RequestHandler_GetResourceRequestHandler(int bid, RObject frame, RObject request, boolean isNavigation, boolean isDownload, String requestInitiator) throws TException {
        return null;
    }

    @Override
    public void ResourceRequestHandler_Dispose(int rrHandler) throws TException {

    }

    @Override
    public RObject ResourceRequestHandler_GetCookieAccessFilter(int rrHandler, int bid, RObject frame, RObject request) throws TException {
        return null;
    }

    @Override
    public void CookieAccessFilter_Dispose(int filter) throws TException {

    }

    @Override
    public boolean CookieAccessFilter_CanSendCookie(int filter, int bid, RObject frame, RObject request, List<String> cookie) throws TException {
        return false;
    }

    @Override
    public boolean CookieAccessFilter_CanSaveCookie(int filter, int bid, RObject frame, RObject request, RObject response, List<String> cookie) throws TException {
        return false;
    }

    @Override
    public boolean ResourceRequestHandler_OnBeforeResourceLoad(int rrHandler, int bid, RObject frame, RObject request) throws TException {
        return false;
    }

    @Override
    public RObject ResourceRequestHandler_GetResourceHandler(int rrHandler, int bid, RObject frame, RObject request) throws TException {
        return null;
    }

    @Override
    public void ResourceHandler_Dispose(int resourceHandler) throws TException {

    }

    @Override
    public boolean ResourceHandler_ProcessRequest(int resourceHandler, RObject request, RObject callback) throws TException {
        return false;
    }

    @Override
    public ResponseHeaders ResourceHandler_GetResponseHeaders(int resourceHandler, RObject response) throws TException {
        return null;
    }

    @Override
    public ResponseData ResourceHandler_ReadResponse(int resourceHandler, int bytes_to_read, RObject callback) throws TException {
        return null;
    }

    @Override
    public void ResourceHandler_Cancel(int resourceHandler) throws TException {

    }

    @Override
    public String ResourceRequestHandler_OnResourceRedirect(int rrHandler, int bid, RObject frame, RObject request, RObject response, String new_url) throws TException {
        return null;
    }

    @Override
    public boolean ResourceRequestHandler_OnResourceResponse(int rrHandler, int bid, RObject frame, RObject request, RObject response) throws TException {
        return false;
    }

    @Override
    public void ResourceRequestHandler_OnResourceLoadComplete(int rrHandler, int bid, RObject frame, RObject request, RObject response, String status, long receivedContentLength) throws TException {

    }

    @Override
    public boolean ResourceRequestHandler_OnProtocolExecution(int rrHandler, int bid, RObject frame, RObject request, boolean allowOsExecution) throws TException {
        return false;
    }

    @Override
    public boolean MessageRouterHandler_onQuery(RObject handler, int bid, RObject frame, long queryId, String request, boolean persistent, RObject queryCallback) throws TException {
        return false;
    }

    @Override
    public void MessageRouterHandler_onQueryCanceled(RObject handler, int bid, RObject frame, long queryId) throws TException {

    }

    @Override
    public void MessageRouterHandler_Dispose(int handler) throws TException {

    }

    @Override
    public RObject SchemeHandlerFactory_CreateHandler(int schemeHandlerFactory, int bid, RObject frame, String scheme_name, RObject request) throws TException {
        return null;
    }

    @Override
    public void SchemeHandlerFactory_Dispose(int schemeHandlerFactory) throws TException {

    }

    @Override
    public void CompletionCallback_OnComplete(int completionCallback) throws TException {

    }

    @Override
    public RObject RequestContextHandler_GetResourceRequestHandler(int handler, int bid, RObject frame, RObject request, boolean isNavigation, boolean isDownload, String requestInitiator) throws TException {
        return null;
    }

    @Override
    public boolean CookieVisitor_Visit(int visitor, Cookie cookie, int count, int total) throws TException {
        return false;
    }

    @Override
    public void CookieVisitor_Dispose(int visitor) throws TException {

    }
}
