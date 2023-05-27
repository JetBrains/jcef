package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.router.RemoteMessageRouter;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.handler.*;
import org.cef.misc.CefLog;

import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteClient implements CefClient {
    private static AtomicInteger ourCounter = new AtomicInteger(0);

    private final int myCid;
    private final RpcExecutor myService;
    private final BrowserTracker myTracker;
    private RemoteBrowser myRemoteBrowser;
    private volatile boolean myIsNativeBrowserCreated = false;
    private volatile boolean myIsNativeBrowserClosed = false;

    private CefContextMenuHandler contextMenuHandler_ = null;
    private CefDialogHandler dialogHandler_ = null;
    private CefDisplayHandler displayHandler_ = null;
    private CefDownloadHandler downloadHandler_ = null;
    private CefDragHandler dragHandler_ = null;
    private CefFocusHandler focusHandler_ = null;
    private CefPermissionHandler permissionHandler_ = null;
    private CefJSDialogHandler jsDialogHandler_ = null;
    private CefKeyboardHandler keyboardHandler_ = null;
    private CefPrintHandler printHandler_ = null;
    private CefRequestHandler requestHandler_ = null;
    private CefNativeRenderHandler renderHandler_ = null;
    private CefWindowHandler windowHandler_ = null;

    final MultiHandler<CefLifeSpanHandler> hLifeSpan = new MultiHandler<>();
    final MultiHandler<CefLoadHandler> hLoad = new MultiHandler<>();

    // MessageRouter support
    private Vector<RemoteMessageRouter> msgRouters = new Vector<>();

    public interface BrowserTracker {
        void register(RemoteBrowser browser);
        void unregister(int bid);
    }
    public RemoteClient(RpcExecutor service, BrowserTracker tracker) {
        myCid = ourCounter.getAndIncrement();
        myService = service;
        myTracker = tracker;

        hLifeSpan.addHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                myIsNativeBrowserCreated = true;
            }
            @Override
            public void onBeforeClose(CefBrowser browser) {
                myIsNativeBrowserClosed = true;
            }
        });
    }

    protected BrowserTracker getTracker() { return myTracker; }

    protected CefContextMenuHandler getContextMenuHandler() {
        return contextMenuHandler_;
    }

    protected CefDialogHandler getDialogHandler() {
        return dialogHandler_;
    }

    protected CefDisplayHandler getDisplayHandler() {
        return displayHandler_;
    }

    protected CefDownloadHandler getDownloadHandler() {
        return downloadHandler_;
    }

    protected CefDragHandler getDragHandler() {
        return dragHandler_;
    }

    protected CefFocusHandler getFocusHandler() {
        return focusHandler_;
    }

    protected CefPermissionHandler getPermissionHandler() {
        return permissionHandler_;
    }

    protected CefJSDialogHandler getJSDialogHandler() {
        return jsDialogHandler_;
    }

    protected CefKeyboardHandler getKeyboardHandler() {
        return keyboardHandler_;
    }

    protected CefPrintHandler getPrintHandler() {
        return printHandler_;
    }

    protected CefRenderHandler getRenderHandler() {
        return renderHandler_;
    }

    protected CefRequestHandler getRequestHandler() {
        return requestHandler_;
    }

    protected CefWindowHandler getWindowHandler() {
        return windowHandler_;
    }


    //
    // Public API
    //

    public int getCid() { return myCid; }

    public boolean isNativeBrowserCreated() { return myIsNativeBrowserCreated; }
    public boolean isNativeBrowserClosed() { return myIsNativeBrowserClosed; }

    public void setRenderHandler(CefNativeRenderHandler renderHandler) { this.renderHandler_ = renderHandler; }

    //
    // CefClient
    //

    // Browser creation
    public RemoteBrowser createBrowser(String url, boolean isTransparent, CefRequestContext context) {
        // TODO: support context
        RemoteBrowser result = new RemoteBrowser(myService, this, url);
        return result;
    }

    // Handlers management

    @Override
    public CefClient addLifeSpanHandler(CefLifeSpanHandler handler) {
        hLifeSpan.addHandler(handler);
        return this;
    }

    @Override
    public void removeLifeSpanHandler() {
        // TODO: use lifeSpanHandler_.removeHandler(handler);
        hLifeSpan.removeAllHandlers();
    }

    @Override
    public CefClient addLoadHandler(CefLoadHandler loadHandler) {
        hLoad.addHandler(loadHandler);
        return this;
    }

    @Override
    public void removeLoadHandler() {
        // TODO: use loadHandler_.removeHandler(handler);
        hLoad.removeAllHandlers();
    }

    @Override
    public CefClient addDisplayHandler(CefDisplayHandler displayHandler) {
        if (displayHandler_ != null && !Objects.equals(displayHandler_, displayHandler))
            CefLog.Warn("DisplayHandler will be replaced.");
        displayHandler_ = displayHandler;
        return this;
    }

    @Override
    public void removeDisplayHandler() { displayHandler_ = null; }

    @Override
    public CefClient addRequestHandler(CefRequestHandler requestHandler) {
        if (requestHandler_ != null && !Objects.equals(requestHandler_, requestHandler))
            CefLog.Warn("RequestHandler will be replaced.");
        requestHandler_ = requestHandler;
        return this;
    }

    @Override
    public void removeRequestHandler() { requestHandler_ = null; }

    @Override
    public CefClient addContextMenuHandler(CefContextMenuHandler handler) {
        return null;
    }

    @Override
    public void removeContextMenuHandler() {

    }

    @Override
    public CefClient addDialogHandler(CefDialogHandler handler) {
        return null;
    }

    @Override
    public void removeDialogHandler() {

    }

    @Override
    public CefClient addDownloadHandler(CefDownloadHandler handler) {
        return null;
    }

    @Override
    public void removeDownloadHandler() {

    }

    @Override
    public CefClient addDragHandler(CefDragHandler handler) {
        return null;
    }

    @Override
    public void removeDragHandler() {

    }

    @Override
    public CefClient addFocusHandler(CefFocusHandler handler) {
        return null;
    }

    @Override
    public void removeFocusHandler() {

    }

    @Override
    public CefClient addPermissionHandler(CefPermissionHandler handler) {
        return null;
    }

    @Override
    public void removePermissionHandler() {

    }

    @Override
    public CefClient addJSDialogHandler(CefJSDialogHandler handler) {
        return null;
    }

    @Override
    public void removeJSDialogHandler() {

    }

    @Override
    public CefClient addKeyboardHandler(CefKeyboardHandler handler) {
        return null;
    }

    @Override
    public void removeKeyboardHandler() {

    }

    @Override
    public CefClient addPrintHandler(CefPrintHandler handler) {
        return null;
    }

    @Override
    public void removePrintHandler() {

    }

    @Override
    public void addMessageRouter(CefMessageRouter messageRouter) {

    }

    @Override
    public void removeMessageRouter(CefMessageRouter messageRouter) {

    }

    //
    // CefMessageRouter
    //

    // NOTE: Stores messageRouter ref.
    public void addMessageRouter(RemoteMessageRouter messageRouter) {
        // NOTE: we create RemoteMessageRouter via static factory method and then configure it
        // with java handlers (internally will remote wrappers over java objects). CefMessageRouter is used only to
        // add/remove handlers. So we can't create remote wrapper over pure CefMessageRouter here, since it's not a "handler".
        msgRouters.add(messageRouter);
        if (myRemoteBrowser != null && myRemoteBrowser.getBid() >= 0)
            messageRouter.addToBrowser(myRemoteBrowser.getBid());
    }

    public void removeMessageRouter(RemoteMessageRouter messageRouter) {
        if (myRemoteBrowser != null && myRemoteBrowser.getBid() >= 0)
            messageRouter.removeFromBrowser(myRemoteBrowser.getBid());
        msgRouters.remove(messageRouter);
    }

    @Override
    public void dispose() {
        CefLog.Debug("RemoteClient: dispose cid=%d bid=%d", myCid, myRemoteBrowser != null ? myRemoteBrowser.getBid() : -1);

        // 1. Cleanup routers
        msgRouters.clear();

        // 2. Cleanup rendering stuff
        if (renderHandler_ != null)
            renderHandler_.disposeNativeResources();

        myRemoteBrowser = null;
    }

    @Override
    public String toString() {
        return "CefRemoteClient_" + myCid;
    }
}
