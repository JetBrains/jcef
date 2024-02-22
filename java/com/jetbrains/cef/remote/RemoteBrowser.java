package com.jetbrains.cef.remote;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefDevToolsClient;
import org.cef.browser.CefFrame;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefNativeRenderHandler;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefWindowHandler;
import org.cef.input.CefCompositionUnderline;
import org.cef.input.CefTouchEvent;
import org.cef.misc.CefLog;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.misc.CefRange;
import org.cef.network.CefRequest;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

public class RemoteBrowser implements CefBrowser {
    private final RpcExecutor myService;
    private final RemoteClient myOwner;
    private final CefClient myCefClient; // will be the "owner" of RemoteClient, needed to override getClient()

    private int myBid = -1;
    private String myUrl = null;
    private Component myComponent;
    private CefNativeRenderHandler myRender;

    private volatile boolean myIsNativeBrowserCreationStarted = false;
    private volatile boolean myIsNativeBrowserCreated = false;
    private volatile boolean myIsClosing = false;
    private volatile boolean myIsClosed = false;
    private volatile int myNativeBrowserIdentifier = Integer.MIN_VALUE;

    public RemoteBrowser(RpcExecutor service, RemoteClient owner, CefClient cefClient, String url) {
        myService = service;
        myOwner = owner;
        myCefClient = cefClient;
        myUrl = url;
    }

    public int getBid() { return myBid; }
    public int getCid() { return myOwner.getCid(); }
    public RemoteClient getOwner() { return myOwner; }

    public boolean isNativeBrowserCreationStarted() { return myIsNativeBrowserCreationStarted; }
    public boolean isNativeBrowserCreated() { return myIsNativeBrowserCreated; }
    public int getNativeBrowserIdentifier() { return myNativeBrowserIdentifier; }

    protected void setNativeBrowserCreated(int nativeBrowserIdentifier) {
        // This setter is called from lifespan-handler (of owner)
        myIsNativeBrowserCreated = true;
        myNativeBrowserIdentifier = nativeBrowserIdentifier;
    }

    public void setComponent(Component component, CefNativeRenderHandler renderHandler) {
        myComponent = component;
        myRender = renderHandler;
    }

    @Override
    public void createImmediately() {
        myIsNativeBrowserCreationStarted = true;
        myService.exec((s)->{
            myBid = s.createBrowser(myOwner.getCid(), myUrl);
        });
        if (myBid >= 0)
            myOwner.onBrowserOpened(this);
        else
            CefLog.Error("Can't obtain bid, createBrowser returns %d", myBid);
    }

    @Override
    public Component getUIComponent() {
        return myComponent;
    }

    @Override
    public CefClient getClient() {
        return myCefClient;
    }

    @Override
    public CefRequestContext getRequestContext() {
        return null;
    }

    @Override
    public CefRenderHandler getRenderHandler() { return myRender; }

    @Override
    public CefWindowHandler getWindowHandler() {
        return null;
    }

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    public void goBack() {}

    @Override
    public boolean canGoForward() {
        return false;
    }

    @Override
    public void goForward() {}

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public void reload() {
        if (myBid < 0) {
            CefLog.Debug("Skip reload because bid wasn't created.");
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_Reload(myBid);
        });
    }

    @Override
    public void reloadIgnoreCache() {
        if (myBid < 0) {
            CefLog.Debug("Skip reloadIgnoreCache because bid wasn't created.");
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_ReloadIgnoreCache(myBid);
        });
    }

    @Override
    public void stopLoad() {}

    @Override
    public int getIdentifier() {
        return myNativeBrowserIdentifier;
    }

    @Override
    public CefFrame getMainFrame() {
        return null;
    }

    @Override
    public CefFrame getFocusedFrame() {
        return null;
    }

    @Override
    public CefFrame getFrame(long identifier) {
        return null;
    }

    @Override
    public CefFrame getFrame(String name) {
        return null;
    }

    @Override
    public Vector<Long> getFrameIdentifiers() {
        return null;
    }

    @Override
    public Vector<String> getFrameNames() {
        return null;
    }

    @Override
    public int getFrameCount() {
        return 0;
    }

    @Override
    public boolean isPopup() {
        return false;
    }

    @Override
    public boolean hasDocument() {
        return false;
    }

    @Override
    public void viewSource() {}

    @Override
    public void getSource(CefStringVisitor visitor) {}

    @Override
    public void getText(CefStringVisitor visitor) {}

    @Override
    public void loadRequest(CefRequest request) {}

    @Override
    public void loadURL(String url) {
        myUrl = url;
        if (myBid < 0) {
            CefLog.Debug("Skip loadURL because bid wasn't created.");
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_LoadURL(myBid, url);
        });
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        if (myBid < 0) {
            CefLog.Debug("Skip executeJavaScript because bid wasn't created.");
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_ExecuteJavaScript(myBid, code, url, line);
        });
    }

    @Override
    public String getURL() {
        if (myBid < 0) {
            CefLog.Debug("Skip getURL because bid wasn't created, return cached %s", myUrl);
            return myUrl;
        }
        if (myIsClosing)
            return myUrl;

        return myService.execObj((s)->{
            return s.Browser_GetURL(myBid);
        });
    }

    @Override
    public void close(boolean force) {
        if (myIsClosing)
            return;
        myIsClosing = true;
        if (myBid < 0)
            return;

        myService.exec((s)->{
            s.closeBrowser(myBid);
        });
        myIsClosing = true;
    }

    @Override
    public void setCloseAllowed() {}

    @Override
    public boolean doClose() { return false; }

    @Override
    public void onBeforeClose() { myIsClosed = true; } // Called from lifespan handler (before native browser disposed).

    @Override
    public boolean isClosing() { return myIsClosing; }

    @Override
    public boolean isClosed() { return myIsClosed; }

    @Override
    public void setFocus(boolean enable) {

    }

    @Override
    public void setWindowVisibility(boolean visible) {

    }

    @Override
    public double getZoomLevel() {
        return 0;
    }

    @Override
    public void setZoomLevel(double zoomLevel) {

    }

    @Override
    public void runFileDialog(CefDialogHandler.FileDialogMode mode, String title, String defaultFilePath, Vector<String> acceptFilters, CefRunFileDialogCallback callback) {

    }

    @Override
    public void startDownload(String url) {

    }

    @Override
    public void print() {

    }

    @Override
    public void printToPDF(String path, CefPdfPrintSettings settings, CefPdfPrintCallback callback) {

    }

    @Override
    public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {

    }

    @Override
    public void stopFinding(boolean clearSelection) {

    }

    @Override
    public CefBrowser getDevTools() {
        return null;
    }

    @Override
    public CefBrowser getDevTools(Point inspectAt) {
        return null;
    }

    @Override
    public CefDevToolsClient getDevToolsClient() {
        return null;
    }

    @Override
    public void replaceMisspelling(String word) {

    }

    @Override
    public void wasResized(int width, int height) {
        // TODO: remember size in render-handler
        if (myBid < 0) {
            CefLog.Debug("Skip wasResized because bid wasn't created");
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_WasResized(myBid, width, height);
        });
    }

    @Override
    public void notifyScreenInfoChanged() {

    }

    @Override
    public void sendKeyEvent(KeyEvent e) {
        if (myBid < 0) {
            CefLog.Debug("Skip sendKeyEvent because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            // TODO: get e.scancode via reflection (windows only)
            s.Browser_SendKeyEvent(myBid, e.getID(), e.getModifiersEx(), (short)e.getKeyChar(), 0, e.getKeyCode());
        });
    }

    @Override
    public void sendMouseEvent(MouseEvent e) {
        if (myBid < 0) {
            CefLog.Debug("Skip sendMouseEvent because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_SendMouseEvent(myBid, e.getID(), e.getX(), e.getY(), e.getModifiersEx(), e.getClickCount(), e.getButton());
        });
    }

    @Override
    public void sendMouseWheelEvent(MouseWheelEvent e) {
        if (myBid < 0) {
            CefLog.Debug("Skip sendMouseWheelEvent because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_SendMouseWheelEvent(myBid, e.getScrollType(), e.getX(), e.getY(), e.getModifiersEx(), e.getWheelRotation(), e.getUnitsToScroll());
        });
    }

    @Override
    public void sendTouchEvent(CefTouchEvent e) {
        CefLog.Error("UNIMPLEMENTED: sendTouchEvent");
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        return null;
    }

    @Override
    public void ImeSetComposition(String s, List<CefCompositionUnderline> list, CefRange cefRange, CefRange cefRange1) {
        CefLog.Error("ImeSetComposition is not implemented");
    }

    @Override
    public void ImeCommitText(String s, CefRange cefRange, int i) {
        CefLog.Error("ImeCommitText is not implemented");
    }

    @Override
    public void ImeFinishComposingText(boolean b) {
        CefLog.Error("ImeFinishComposingText is not implemented");
    }

    @Override
    public void ImeCancelComposing() {
        CefLog.Error("ImeCancelComposing is not implemented");
    }

    @Override
    public void setWindowlessFrameRate(int frameRate) {

    }

    @Override
    public CompletableFuture<Integer> getWindowlessFrameRate() {
        return null;
    }

    @Override
    public String toString() {
        return "RemoteBrowser_" + myBid;
    }
}
