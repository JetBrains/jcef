package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.network.RemoteRequestContext;
import com.jetbrains.cef.remote.router.RemoteMessageRouter;
import com.jetbrains.cef.remote.router.RemoteMessageRouterImpl;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.handler.*;
import org.cef.misc.CefLog;

import java.awt.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteClient {
    private static AtomicInteger ourCounter = new AtomicInteger(0);

    private final int myCid;
    private final RpcExecutor myService;
    private final Map<Integer, RemoteBrowser> ourBid2Browser; // global storage
    private final Map<Integer, RemoteBrowser> myNativeIdentifier2Browser = new ConcurrentHashMap<>();
    private final List<RemoteBrowser> myBrowsers = Collections.synchronizedList(new ArrayList<>());

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
    private CefLoadHandler loadHandler_ = null;

    protected final MultiHandler<CefLifeSpanHandler> hLifeSpan = new MultiHandler<>(); // always presented
    private int myHandlersMask = 0;

    // MessageRouter support
    private Vector<RemoteMessageRouterImpl> msgRouters = new Vector<>();

    public RemoteClient(RpcExecutor service, Map<Integer, RemoteBrowser> bid2browser) {
        myCid = ourCounter.getAndIncrement();
        myService = service;
        ourBid2Browser = bid2browser;
    }

    // Called from lifespan handler when native browser is created on server side.
    protected void onAfterCreated(RemoteBrowser browser, int nativeBrowserIdentifier) {
        browser.setNativeBrowserCreated(nativeBrowserIdentifier);
        myNativeIdentifier2Browser.put(nativeBrowserIdentifier, browser);
        hLifeSpan.handle(lsh->lsh.onAfterCreated(browser));
        CefLog.Debug("Browser %s was created (native server-side part).", browser);
    }

    // Called from lifespan handler when native browser is disposed on server side.
    protected void onBeforeClosed(RemoteBrowser browser) {
        hLifeSpan.handle(lsh->lsh.onBeforeClose(browser));

        if (!myBrowsers.remove(browser))
            CefLog.Error("Browser %s already was removed.", browser);
        if (myNativeIdentifier2Browser.remove(browser.getNativeBrowserIdentifier()) == null)
            CefLog.Error("Browser with native id %d already was removed.", browser.getNativeBrowserIdentifier());

        final int bid = browser.getBid();
        if (bid >= 0) {
            RemoteBrowser removed = ourBid2Browser.remove(bid);
            if (removed == null)
                CefLog.Error("Unregister bid: bid=%d was already removed.");
        } else
            CefLog.Error("Can't unregister invalid bid %d", bid);

        browser.onBeforeClose();
        CefLog.Debug("Browser %s was closed (native server-side part).", browser);
    }

    // Called when new bid obtained from server.
    protected void onNewBid(RemoteBrowser browser) {
        int bid = browser.getBid();
        assert bid >= 0;
        ourBid2Browser.put(bid, browser);
    }

    //
    // Handler getters.
    //
    public CefContextMenuHandler getContextMenuHandler() {
        return contextMenuHandler_;
    }
    public CefDialogHandler getDialogHandler() {
        return dialogHandler_;
    }
    public CefDisplayHandler getDisplayHandler() {
        return displayHandler_;
    }
    public CefDownloadHandler getDownloadHandler() {
        return downloadHandler_;
    }
    public CefDragHandler getDragHandler() {
        return dragHandler_;
    }
    public CefFocusHandler getFocusHandler() {
        return focusHandler_;
    }
    public CefPermissionHandler getPermissionHandler() {
        return permissionHandler_;
    }
    public CefJSDialogHandler getJSDialogHandler() {
        return jsDialogHandler_;
    }
    public CefKeyboardHandler getKeyboardHandler() {
        return keyboardHandler_;
    }
    public CefPrintHandler getPrintHandler() {
        return printHandler_;
    }
    public CefRequestHandler getRequestHandler() {
        return requestHandler_;
    }
    public CefLoadHandler getLoadHandler() { return loadHandler_; }

    //
    // Public API
    //

    public RemoteBrowser getRemoteBrowser(int nativeIdentifier) { return myNativeIdentifier2Browser.get(nativeIdentifier); }
    public RemoteBrowser[] getAllBrowsers() {
        // returns only active browsers (with created native part and not closed)
        return myNativeIdentifier2Browser.values().toArray(new RemoteBrowser[]{});
    }

    public int getCid() { return myCid; }

    public int getHandlersMask() { return myHandlersMask; }

    //
    // CefClient
    //

    public RemoteBrowser createBrowser(String url, CefRequestContext context, CefClient client, CefNativeRenderHandler renderHandler, Component component) {
        RemoteRequestContext ctx = null;
        if (context instanceof RemoteRequestContext)
            ctx = (RemoteRequestContext)context;
        else if (context != null)
            CefLog.Error("Unsupported class %s, will be used default (global) request context. Please use RemoteRequestContext.", context.getClass());

        RemoteBrowser browser = new RemoteBrowser(myService, this, client, url, ctx);
        browser.setComponent(component, renderHandler);
        myBrowsers.add(browser);
        return browser;
    }

    public RemoteBrowser createBrowser(String url, CefRequestContext context, CefClient client, CefRendering rendering) {
        if (rendering instanceof CefRendering.CefRenderingWithHandler) {
            CefRendering.CefRenderingWithHandler rh = (CefRendering.CefRenderingWithHandler) rendering;
            if (rh.getRenderHandler() instanceof CefNativeRenderHandler) {
                return createBrowser(url, context, client, (CefNativeRenderHandler)rh.getRenderHandler(), rh.getComponent());
            }
            throw new IllegalStateException("Can't create remote browser with render-handler: " + rh.getRenderHandler());
        }
        throw new IllegalStateException("Can't create remote browser with rendering: " + rendering);
    }

    // Handlers management
    private void _updateMask(Object handler, int handlerMask) {
        if (handler != null)
            myHandlersMask |= handlerMask;
        else
            myHandlersMask &= ~handlerMask;
    }

    public void addLifeSpanHandler(CefLifeSpanHandler handler) {
        hLifeSpan.addHandler(handler);
    }

    public void removeAllLifeSpanHandlers() {
        hLifeSpan.removeAllHandlers();
    }

    public void addLoadHandler(CefLoadHandler loadHandler) {
        if (loadHandler_ != null && !Objects.equals(loadHandler_, loadHandler))
            CefLog.Warn("loadHandler_ will be replaced.");
        loadHandler_ = loadHandler;
        _updateMask(loadHandler_, HandlerMasks.Load.val());
    }

    public void removeLoadHandler() {
        loadHandler_ = null;
        _updateMask(loadHandler_, HandlerMasks.Load.val());
    }

    public void addDisplayHandler(CefDisplayHandler displayHandler) {
        if (displayHandler_ != null && !Objects.equals(displayHandler_, displayHandler))
            CefLog.Warn("displayHandler_ will be replaced.");
        displayHandler_ = displayHandler;
        _updateMask(displayHandler_, HandlerMasks.Display.val());
    }

    public void removeDisplayHandler() {
        displayHandler_ = null;
        _updateMask(displayHandler_, HandlerMasks.Display.val());
    }

    public void addRequestHandler(CefRequestHandler requestHandler) {
        if (requestHandler_ != null && !Objects.equals(requestHandler_, requestHandler))
            CefLog.Warn("requestHandler_ will be replaced.");
        requestHandler_ = requestHandler;
        _updateMask(requestHandler_, HandlerMasks.Request.val());
    }

    public void removeRequestHandler() {
        requestHandler_ = null;
        _updateMask(requestHandler_, HandlerMasks.Request.val());
    }

    public void addContextMenuHandler(CefContextMenuHandler handler) {
        if (contextMenuHandler_ != null && !Objects.equals(contextMenuHandler_, handler))
            CefLog.Warn("contextMenuHandler_ will be replaced.");
        contextMenuHandler_ = handler;
        _updateMask(contextMenuHandler_, HandlerMasks.ContextMenu.val());
    }

    public void removeContextMenuHandler() {
        contextMenuHandler_ = null;
        _updateMask(contextMenuHandler_, HandlerMasks.ContextMenu.val());
    }

    public void addDialogHandler(CefDialogHandler handler) {
        if (dialogHandler_ != null && !Objects.equals(dialogHandler_, handler))
            CefLog.Warn("dialogHandler_ will be replaced.");
        dialogHandler_ = handler;
        _updateMask(dialogHandler_, HandlerMasks.Dialog.val());
    }

    public void removeDialogHandler() {
        dialogHandler_ = null;
        _updateMask(dialogHandler_, HandlerMasks.Dialog.val());
    }

    public void addDownloadHandler(CefDownloadHandler handler) {
        if (downloadHandler_ != null && !Objects.equals(downloadHandler_, handler))
            CefLog.Warn("downloadHandler_ will be replaced.");
        downloadHandler_ = handler;
        _updateMask(downloadHandler_, HandlerMasks.Download.val());
    }

    public void removeDownloadHandler() {
        downloadHandler_ = null;
        _updateMask(downloadHandler_, HandlerMasks.Download.val());
    }

    public void addDragHandler(CefDragHandler handler) {
        if (dragHandler_ != null && !Objects.equals(dragHandler_, handler))
            CefLog.Warn("dragHandler_ will be replaced.");
        dragHandler_ = handler;
        _updateMask(dragHandler_, HandlerMasks.Drag.val());
    }

    public void removeDragHandler() {
        dragHandler_ = null;
        _updateMask(dragHandler_, HandlerMasks.Drag.val());
    }

    public void addFocusHandler(CefFocusHandler handler) {
        if (focusHandler_ != null && !Objects.equals(focusHandler_, handler))
            CefLog.Warn("focusHandler_ will be replaced.");
        focusHandler_ = handler;
        _updateMask(focusHandler_, HandlerMasks.Focus.val());
    }

    public void removeFocusHandler() {
        focusHandler_ = null;
        _updateMask(focusHandler_, HandlerMasks.Focus.val());
    }

    public void addPermissionHandler(CefPermissionHandler handler) {
        if (permissionHandler_ != null && !Objects.equals(permissionHandler_, handler))
            CefLog.Warn("permissionHandler_ will be replaced.");
        permissionHandler_ = handler;
        _updateMask(permissionHandler_, HandlerMasks.Permission.val());
    }

    public void removePermissionHandler() {
        permissionHandler_ = null;
        _updateMask(permissionHandler_, HandlerMasks.Permission.val());
    }

    public void addJSDialogHandler(CefJSDialogHandler handler) {
        if (jsDialogHandler_ != null && !Objects.equals(jsDialogHandler_, handler))
            CefLog.Warn("jsDialogHandler_ will be replaced.");
        jsDialogHandler_ = handler;
        _updateMask(jsDialogHandler_, HandlerMasks.JSDialog.val());
    }

    public void removeJSDialogHandler() {
        jsDialogHandler_ = null;
        _updateMask(jsDialogHandler_, HandlerMasks.JSDialog.val());
    }

    public void addKeyboardHandler(CefKeyboardHandler handler) {
        if (keyboardHandler_ != null && !Objects.equals(keyboardHandler_, handler))
            CefLog.Warn("keyboardHandler_ will be replaced.");
        keyboardHandler_ = handler;
        _updateMask(keyboardHandler_, HandlerMasks.Keyboard.val());
    }

    public void removeKeyboardHandler() {
        keyboardHandler_ = null;
        _updateMask(keyboardHandler_, HandlerMasks.Keyboard.val());
    }

    public void addPrintHandler(CefPrintHandler handler) {
        if (printHandler_ != null && !Objects.equals(printHandler_, handler))
            CefLog.Warn("printHandler_ will be replaced.");
        printHandler_ = handler;
        _updateMask(printHandler_, HandlerMasks.Print.val());
    }

    public void removePrintHandler() {
        printHandler_ = null;
        _updateMask(printHandler_, HandlerMasks.Print.val());
    }

    //
    // CefMessageRouter
    //

    public void addMessageRouter(CefMessageRouter messageRouter) {
        // NOTE: we create RemoteMessageRouter via static factory method and then configure it
        // with java handlers (internally will remote wrappers over java objects). CefMessageRouter is used only to
        // add/remove handlers. So we can't create remote wrapper over "java" CefMessageRouter here.
        RemoteMessageRouter router = (RemoteMessageRouter)messageRouter;
        msgRouters.add(router.getImpl());
        myBrowsers.forEach(rb -> {
            final int bid = rb != null ? rb.getBid() : -1;
            if (bid >= 0)
                router.getImpl().addToBrowser(bid);
        });
    }

    public void removeMessageRouter(CefMessageRouter messageRouter) {
        RemoteMessageRouter router = (RemoteMessageRouter)messageRouter;
        myBrowsers.forEach(rb -> {
            final int bid = rb != null ? rb.getBid() : -1;
            if (bid >= 0)
                router.getImpl().removeFromBrowser(bid);
        });
        msgRouters.remove(router.getImpl());
    }

    public void dispose() {
        CefLog.Debug("RemoteClient: dispose cid=%d", myCid);

        // 1. Cleanup routers
        // NOTE: CefClientHandler implementation disposes all managed routers. But it seems to be
        // incorrect: router is created outside of client and one router can be used by several clients. So it's better
        // to dispose router as usual (inside finalize or manually (via dispose)) => just clean list here.
        msgRouters.clear();

        // 2. Close browsers
        myBrowsers.forEach(rb -> {
            if (rb != null && !rb.isClosed())
                rb.close(true);
        });
        myBrowsers.clear();
    }

    public String toString() {
        return "RemoteClient_" + myCid;
    }

    enum HandlerMasks {
        Request(1 << 0),
        NativeRender(1 << 1),
        Load(1 << 2),
        ContextMenu(1 << 4),
        Dialog(1 << 5),
        Display(1 << 6),
        Focus(1 << 7),
        Permission(1 << 8),
        JSDialog(1 << 9),
        Keyboard(1 << 10),
        Print(1 << 11),
        Download(1 << 12),
        Drag(1 << 13);

        private final int maskVal;

        HandlerMasks(int maskVal) { this.maskVal = maskVal; }

        int val() { return maskVal; }

        static String toString(int mask) {
            String result = "Lifespan";
            for (HandlerMasks m : HandlerMasks.values()) {
                if ((m.val() & mask) == 0)
                    continue;
                result += ", " + m.name();
            }
            return result;
        }
    }
}
