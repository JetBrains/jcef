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
    private final Map<Integer, CefRemoteBrowser> myBid2RemoteBrowser = new ConcurrentHashMap<>();

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

    public CefRemoteClient() { myCid = ourCounter.getAndIncrement(); }

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

    public void disposeClient() {
        if (renderHandler_ != null) renderHandler_.disposeNativeResources();
    }

    public void registerBrowser(int bid, CefRemoteBrowser remoteBrowser) {
        if (remoteBrowser == null) {
            CefLog.Error("tried to register null remoteClient, bid=%d", bid);
            return;
        }
        myBid2RemoteBrowser.put(bid, remoteBrowser);
    }

    public void unregister(int bid) {
        CefLog.Debug("CefRemoteClient: unregister browser, bid=%d", bid);
        myBid2RemoteBrowser.remove(bid);
    }

    public CefRemoteBrowser getBrowserByBid(int bid) {
        return myBid2RemoteBrowser.get(bid);
    }

    @Override
    public String toString() {
        return "CefRemoteClient_" + myCid;
    }
}
