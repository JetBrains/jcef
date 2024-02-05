package com.jetbrains.cef.remote;

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
    private CefNativeRenderHandler renderHandler_ = null;
    private CefWindowHandler windowHandler_ = null;
    private CefLoadHandler loadHandler_ = null;

    protected final MultiHandler<CefLifeSpanHandler> hLifeSpan = new MultiHandler<>();

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
    protected void onBrowserOpened(RemoteBrowser browser) {
        int bid = browser.getBid();
        if (bid < 0) {
            CefLog.Error("Can't register bid %d", bid);
            return;
        }
        CefLog.Debug("Registered browser %s", browser);
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
    public CefWindowHandler getWindowHandler() {
        return windowHandler_;
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

    //
    // CefClient
    //

    public RemoteBrowser createBrowser(String url, CefRequestContext context, CefClient client, CefNativeRenderHandler renderHandler, Component component) {
        // TODO: support context
        RemoteBrowser browser = new RemoteBrowser(myService, this, client, url);
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
    public void addLifeSpanHandler(CefLifeSpanHandler handler) {
        hLifeSpan.addHandler(handler);
    }

    public void removeLifeSpanHandler() {
        // TODO: use lifeSpanHandler_.removeHandler(handler);
        hLifeSpan.removeAllHandlers();
    }

    public void addLoadHandler(CefLoadHandler loadHandler) {
        if (loadHandler_ != null && !Objects.equals(loadHandler_, loadHandler))
            CefLog.Warn("loadHandler_ will be replaced.");
        loadHandler_ = loadHandler;
    }

    public void removeLoadHandler() { loadHandler_ = null; }

    public void addDisplayHandler(CefDisplayHandler displayHandler) {
        if (displayHandler_ != null && !Objects.equals(displayHandler_, displayHandler))
            CefLog.Warn("displayHandler_ will be replaced.");
        displayHandler_ = displayHandler;
    }

    public void removeDisplayHandler() { displayHandler_ = null; }

    public void addRequestHandler(CefRequestHandler requestHandler) {
        if (requestHandler_ != null && !Objects.equals(requestHandler_, requestHandler))
            CefLog.Warn("requestHandler_ will be replaced.");
        requestHandler_ = requestHandler;
    }

    public void removeRequestHandler() { requestHandler_ = null; }

    public void addContextMenuHandler(CefContextMenuHandler handler) {
        if (contextMenuHandler_ != null && !Objects.equals(contextMenuHandler_, handler))
            CefLog.Warn("contextMenuHandler_ will be replaced.");
        contextMenuHandler_ = handler;
    }

    public void removeContextMenuHandler() {}

    public void addDialogHandler(CefDialogHandler handler) {
        if (dialogHandler_ != null && !Objects.equals(dialogHandler_, handler))
            CefLog.Warn("dialogHandler_ will be replaced.");
        dialogHandler_ = handler;
    }

    public void removeDialogHandler() {}

    public void addDownloadHandler(CefDownloadHandler handler) {
        if (downloadHandler_ != null && !Objects.equals(downloadHandler_, handler))
            CefLog.Warn("downloadHandler_ will be replaced.");
        downloadHandler_ = handler;
    }

    public void removeDownloadHandler() {}

    public void addDragHandler(CefDragHandler handler) {
        if (dragHandler_ != null && !Objects.equals(dragHandler_, handler))
            CefLog.Warn("dragHandler_ will be replaced.");
        dragHandler_ = handler;
    }

    public void removeDragHandler() {}

    public void addFocusHandler(CefFocusHandler handler) {
        if (focusHandler_ != null && !Objects.equals(focusHandler_, handler))
            CefLog.Warn("focusHandler_ will be replaced.");
        focusHandler_ = handler;
    }

    public void removeFocusHandler() {}

    public void addPermissionHandler(CefPermissionHandler handler) {
        if (permissionHandler_ != null && !Objects.equals(permissionHandler_, handler))
            CefLog.Warn("permissionHandler_ will be replaced.");
        permissionHandler_ = handler;
    }

    public void removePermissionHandler() {}

    public void addJSDialogHandler(CefJSDialogHandler handler) {
        if (jsDialogHandler_ != null && !Objects.equals(jsDialogHandler_, handler))
            CefLog.Warn("jsDialogHandler_ will be replaced.");
        jsDialogHandler_ = handler;
    }

    public void removeJSDialogHandler() {}

    public void addKeyboardHandler(CefKeyboardHandler handler) {
        if (keyboardHandler_ != null && !Objects.equals(keyboardHandler_, handler))
            CefLog.Warn("keyboardHandler_ will be replaced.");
        keyboardHandler_ = handler;
    }

    public void removeKeyboardHandler() {}

    public void addPrintHandler(CefPrintHandler handler) {
        if (printHandler_ != null && !Objects.equals(printHandler_, handler))
            CefLog.Warn("printHandler_ will be replaced.");
        printHandler_ = handler;
    }

    public void removePrintHandler() {}

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

        // 2. Cleanup rendering stuff
        if (renderHandler_ != null)
            renderHandler_.disposeNativeResources();

        myBrowsers.forEach(rb -> {
            if (rb != null && !rb.isClosed())
                rb.close(true);
        });
        myBrowsers.clear();
    }

    public String toString() {
        return "RemoteClient_" + myCid;
    }
}
