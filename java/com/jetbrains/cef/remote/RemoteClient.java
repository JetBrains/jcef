package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.router.RemoteMessageRouter;
import com.jetbrains.cef.remote.router.RemoteMessageRouterImpl;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.handler.*;
import org.cef.misc.CefLog;

import java.awt.*;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteClient {
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
    private CefLoadHandler loadHandler_ = null;

    final CefLifeSpanHandler hLifeSpanMain;
    final MultiHandler<CefLifeSpanHandler> hLifeSpan = new MultiHandler<>();

    // MessageRouter support
    private Vector<RemoteMessageRouterImpl> msgRouters = new Vector<>();

    public interface BrowserTracker {
        void register(RemoteBrowser browser);
        void unregister(int bid);
    }
    public RemoteClient(RpcExecutor service, BrowserTracker tracker) {
        myCid = ourCounter.getAndIncrement();
        myService = service;
        myTracker = tracker;

        hLifeSpanMain = new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                myIsNativeBrowserCreated = true;
            }
            @Override
            public void onBeforeClose(CefBrowser browser) {
                myIsNativeBrowserClosed = true;
            }
        };
        hLifeSpan.addHandler(hLifeSpanMain);
    }

    protected BrowserTracker getTracker() { return myTracker; }

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
    public CefRenderHandler getRenderHandler() {
        return renderHandler_;
    }
    public CefRequestHandler getRequestHandler() {
        return requestHandler_;
    }
    public CefWindowHandler getWindowHandler() {
        return windowHandler_;
    }
    public CefLifeSpanHandler getLifeSpanHandler() {
        return hLifeSpanMain;
    }
    public CefLoadHandler getLoadHandler() { return loadHandler_; }

    //
    // Public API
    //

    public RemoteBrowser getRemoteBrowser() { return myRemoteBrowser; }

    public int getCid() { return myCid; }

    public boolean isNativeBrowserCreated() { return myIsNativeBrowserCreated; }
    public boolean isNativeBrowserClosed() { return myIsNativeBrowserClosed; }

    //
    // CefClient
    //

    public RemoteBrowser createBrowser(String url, CefRequestContext context, CefClient client, CefNativeRenderHandler renderHandler, Component component) {
        // TODO: check whether client is disposed
//        if (isDisposed_)
//            throw new IllegalStateException("Can't create browser. CefClient is disposed");
        if (myRemoteBrowser != null) {
            CefLog.Error("Can't create new instance of browser, current %s will be used.", myRemoteBrowser);
        } else {
            // TODO: support context
            myRemoteBrowser = new RemoteBrowser(myService, this, url);
            myRemoteBrowser.setCefClient(client);
            myRemoteBrowser.setComponent(component);
            this.renderHandler_ = renderHandler;
        }
        return myRemoteBrowser;
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
        if (myRemoteBrowser != null && myRemoteBrowser.getBid() >= 0)
            router.getImpl().addToBrowser(myRemoteBrowser.getBid());
    }

    public void removeMessageRouter(CefMessageRouter messageRouter) {
        RemoteMessageRouter router = (RemoteMessageRouter)messageRouter;
        if (myRemoteBrowser != null && myRemoteBrowser.getBid() >= 0)
            router.getImpl().removeFromBrowser(myRemoteBrowser.getBid());
        msgRouters.remove(router.getImpl());
    }

    public void dispose() {
        CefLog.Debug("RemoteClient: dispose cid=%d bid=%d", myCid, myRemoteBrowser != null ? myRemoteBrowser.getBid() : -1);

        // 1. Cleanup routers
        // NOTE: CefClientHandler implementation disposes all managed routers. But it seems to be
        // incorrect: router is created outside of client and one router can be used by several clients. So it's better
        // to dispose router as usual (inside finalize or manually (via dispose)) => just clean list here.
        msgRouters.clear();

        // 2. Cleanup rendering stuff
        if (renderHandler_ != null)
            renderHandler_.disposeNativeResources();

        myRemoteBrowser = null;
    }

    public String toString() {
        return "RemoteClient_" + myCid;
    }
}
