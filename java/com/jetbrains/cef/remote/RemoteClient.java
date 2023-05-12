package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.router.RemoteMessageRouter;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouterBase;
import org.cef.handler.*;
import org.cef.misc.CefLog;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteClient extends CefClientHandlerBase {
    private static AtomicInteger ourCounter = new AtomicInteger(0);

    private final int myCid;
    private final CefServer myServer;
    private RemoteBrowser myRemoteBrowser;

    private CefContextMenuHandler contextMenuHandler_ = null;
    private CefDialogHandler dialogHandler_ = null;
    private CefDisplayHandler displayHandler_ = null;
    private CefDownloadHandler downloadHandler_ = null;
    private CefDragHandler dragHandler_ = null;
    private CefFocusHandler focusHandler_ = null;
    private CefPermissionHandler permissionHandler_ = null;
    private CefJSDialogHandler jsDialogHandler_ = null;
    private CefKeyboardHandler keyboardHandler_ = null;
    private CefLifeSpanHandler lifeSpanHandler_ = null;
    private CefLoadHandler loadHandler_ = null;
    private CefPrintHandler printHandler_ = null;
    private CefRequestHandler requestHandler_ = null;
    private CefNativeRenderHandler renderHandler_ = null;
    private CefWindowHandler windowHandler_ = null;

    // MessageRouter support
    private Vector<RemoteMessageRouter> msgRouters = new Vector<>();

    public RemoteClient(CefServer server) {
        myCid = ourCounter.getAndIncrement();
        myServer = server;
    }

    public void dispose() {
        for (int i = 0; i < msgRouters.size(); i++) {
            msgRouters.get(i).disposeOnServer(); // called in finalize, just for insurance/clearness
        }
        msgRouters.clear();
    }

    public RemoteBrowser createBrowser() {
        if (myRemoteBrowser == null) {
            myRemoteBrowser = myServer.createBrowser(this);
            if (myRemoteBrowser == null)
                CefLog.Error("Can't create remote browser");
        }
        return myRemoteBrowser;
    }

    @Override
    protected CefBrowser getBrowser(int identifier) {
        return null;
    }

    @Override
    protected Object[] getAllBrowser() {
        return new Object[0];
    }

    @Override
    protected CefContextMenuHandler getContextMenuHandler() {
        return contextMenuHandler_;
    }

    @Override
    protected CefDialogHandler getDialogHandler() {
        return dialogHandler_;
    }

    @Override
    protected CefDisplayHandler getDisplayHandler() {
        return displayHandler_;
    }

    @Override
    protected CefDownloadHandler getDownloadHandler() {
        return downloadHandler_;
    }

    @Override
    protected CefDragHandler getDragHandler() {
        return dragHandler_;
    }

    @Override
    protected CefFocusHandler getFocusHandler() {
        return focusHandler_;
    }

    @Override
    protected CefPermissionHandler getPermissionHandler() {
        return permissionHandler_;
    }

    @Override
    protected CefJSDialogHandler getJSDialogHandler() {
        return jsDialogHandler_;
    }

    @Override
    protected CefKeyboardHandler getKeyboardHandler() {
        return keyboardHandler_;
    }

    @Override
    protected CefLifeSpanHandler getLifeSpanHandler() {
        return lifeSpanHandler_;
    }

    @Override
    protected CefLoadHandler getLoadHandler() {
        return loadHandler_;
    }

    @Override
    protected CefPrintHandler getPrintHandler() {
        return printHandler_;
    }

    @Override
    protected CefRenderHandler getRenderHandler() {
        return renderHandler_;
    }

    @Override
    protected CefRequestHandler getRequestHandler() {
        return requestHandler_;
    }

    @Override
    protected CefWindowHandler getWindowHandler() {
        return windowHandler_;
    }


    //
    // Public API
    //

    public int getCid() { return myCid; }

    public void setLifeSpanHandler(CefLifeSpanHandler lifeSpanHandler) {
        this.lifeSpanHandler_ = lifeSpanHandler;
    }

    public void setLoadHandler(CefLoadHandler loadHandler) {
        this.loadHandler_ = loadHandler;
    }

    public void setRenderHandler(CefNativeRenderHandler renderHandler) {
        this.renderHandler_ = renderHandler;
    }

    public void setDisplayHandler(CefDisplayHandler displayHandler) {
        this.displayHandler_ = displayHandler;
    }

    public void setRequestHandler(CefRequestHandler requestHandler) {
        this.requestHandler_ = requestHandler;
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
        if (myRemoteBrowser != null)
            messageRouter.addToBrowser(myRemoteBrowser.getBid());
    }

    public void removeMessageRouter(RemoteMessageRouter messageRouter) {
        if (myRemoteBrowser != null)
            messageRouter.removeFromBrowser(myRemoteBrowser.getBid());
        msgRouters.remove(messageRouter);
    }

    @Override
    public String toString() {
        return "CefRemoteClient_" + myCid;
    }

    void disposeClient() {
        CefLog.Debug("CefRemoteClient: dispose cid=%d", myCid);
        if (renderHandler_ != null)
            renderHandler_.disposeNativeResources();
        myRemoteBrowser = null;
    }
}
