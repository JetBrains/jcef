package com.jetbrains.cef.remote;

import org.cef.browser.CefBrowser;
import org.cef.handler.*;
import org.cef.misc.CefLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CefRemoteClient extends CefClientHandlerBase {
    private static AtomicInteger ourCounter = new AtomicInteger(0);

    private final int myCid;
    private final CefServer myServer;
    private CefRemoteBrowser myRemoteBrowser;

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

    public CefRemoteClient(CefServer server) {
        myCid = ourCounter.getAndIncrement();
        myServer = server;
    }

    public CefRemoteBrowser createBrowser() {
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
