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
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefWindowHandler;
import org.cef.input.CefCompositionUnderline;
import org.cef.input.CefTouchEvent;
import org.cef.misc.CefLog;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.misc.CefRange;
import org.cef.network.CefRequest;
import org.slf4j.spi.LoggingEventBuilder;

import javax.swing.*;
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

    private int myBid = -1;
    private String myUrl = null;
    private Component myComponent;
    private CefClient myCefClient;

    public RemoteBrowser(RpcExecutor service, RemoteClient owner, String url) {
        myService = service;
        myOwner = owner;
        myUrl = url;
    }

    public int getBid() { return myBid; }
    public int getCid() { return myOwner.getCid(); }
    public RemoteClient getOwner() { return myOwner; }
    public CefClient getCefClient() { return myCefClient; }

    public boolean isNativeBrowserCreated() { return myOwner.isNativeBrowserCreated(); }
    public boolean isNativeBrowserClosed() { return myOwner.isNativeBrowserClosed(); }

    public void setComponent(Component component) { myComponent = component; }
    public void setCefClient(CefClient client) { myCefClient = client; }


    @Override
    public void createImmediately() {
        myService.exec((s)->{
            myBid = s.createBrowser(myOwner.getCid(), myUrl);
        });
        if (myBid < 0)
            CefLog.Error("Can't obtain bid, result=%d", myBid);
        else
            myOwner.getTracker().register(this);
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
    public CefRenderHandler getRenderHandler() {
        return myOwner.getRenderHandler();
    }

    @Override
    public CefWindowHandler getWindowHandler() {
        return null;
    }

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    public void goBack() {

    }

    @Override
    public boolean canGoForward() {
        return false;
    }

    @Override
    public void goForward() {

    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public void reload() {
        if (myBid < 0) {
            CefLog.Debug("Skip reload because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        myService.exec((s)->{
            s.Browser_Reload(myBid);
        });
    }

    @Override
    public void reloadIgnoreCache() {
        if (myBid < 0) {
            CefLog.Debug("Skip reloadIgnoreCache because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        myService.exec((s)->{
            s.Browser_ReloadIgnoreCache(myBid);
        });
    }

    @Override
    public void stopLoad() {

    }

    @Override
    public int getIdentifier() {
        return 0;
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
    public void viewSource() {

    }

    @Override
    public void getSource(CefStringVisitor visitor) {

    }

    @Override
    public void getText(CefStringVisitor visitor) {

    }

    @Override
    public void loadRequest(CefRequest request) {

    }

    @Override
    public void loadURL(String url) {
        if (myBid < 0) {
            CefLog.Debug("Skip loadURL because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        myService.exec((s)->{
            s.Browser_LoadURL(myBid, url);
        });
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        if (myBid < 0) {
            CefLog.Debug("Skip executeJavaScript because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        myService.exec((s)->{
            s.Browser_ExecuteJavaScript(myBid, code, url, line);
        });
    }

    @Override
    public String getURL() {
        if (myBid < 0) {
            CefLog.Debug("Skip getURL because remote browser wasn't created, bid=%d", myBid);
            return myUrl;
        }
        return myService.execObj((s)->{
            return s.Browser_GetURL(myBid);
        });
    }

    @Override
    public void close(boolean force) {
        if (myBid >= 0) {
            if (myOwner.getTracker() != null)
                myOwner.getTracker().unregister(myBid);

            myService.exec((s)->{
                // TODO: should we support force flag ? does it affect smth in OSR ?
                s.closeBrowser(myBid);
            });
        }
        getOwner().dispose();
    }

    @Override
    public void setCloseAllowed() {

    }

    @Override
    public boolean doClose() {
        return false;
    }

    @Override
    public void onBeforeClose() {

    }

    @Override
    public boolean isClosing() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

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
        if (myBid < 0) {
            CefLog.Debug("Skip wasResized because remote browser wasn't created, bid=%d", myBid);
            return;
        }
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
