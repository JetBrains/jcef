package com.jetbrains.cef.remote;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefWindowHandler;
import org.cef.input.CefTouchEvent;
import org.cef.misc.CefLog;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.network.CefRequest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

public class RemoteBrowser implements CefBrowser {
    private final RpcExecutor myService;
    private final RemoteClient myOwner;

    // TODO: check all myBid usages (compare with -1)
    private int myBid = -1;
    private String myUrl = null;
    private JComponent myComponent;

    public RemoteBrowser(RpcExecutor service, RemoteClient owner, String url) {
        myService = service;
        myOwner = owner;
        myUrl = url;
    }

    public int getBid() { return myBid; }
    public int getCid() { return myOwner.getCid(); }
    public RemoteClient getOwner() { return myOwner; }

    public boolean isNativeBrowserCreated() { return myOwner.isNativeBrowserCreated(); }
    public boolean isNativeBrowserClosed() { return myOwner.isNativeBrowserClosed(); }

    public void setComponent(JComponent component) { myComponent = component; }

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
        return myOwner;
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
        myService.exec((s)->{
            s.Browser_Reload(myBid);
        });
    }

    @Override
    public void reloadIgnoreCache() {
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
        myService.exec((s)->{
            s.Browser_LoadURL(myBid, url);
        });
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        myService.exec((s)->{
            s.Browser_ExecuteJavaScript(myBid, code, url, line);
        });
    }

    @Override
    public String getURL() {
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
    public void replaceMisspelling(String word) {

    }

    @Override
    public void wasResized(int width, int height) {
        myService.exec((s)->{
            s.Browser_WasResized(myBid, width, height);
        });
    }

    @Override
    public void notifyScreenInfoChanged() {

    }

    @Override
    public void sendKeyEvent(KeyEvent e) {
        myService.exec((s)->{
            // TODO: get e.scancode via reflection (windows only)
            s.Browser_SendKeyEvent(myBid, e.getID(), e.getModifiersEx(), (short)e.getKeyChar(), 0, e.getKeyCode());
        });
    }

    @Override
    public void sendMouseEvent(MouseEvent e) {
        myService.exec((s)->{
            s.Browser_SendMouseEvent(myBid, e.getID(), e.getX(), e.getY(), e.getModifiersEx(), e.getClickCount(), e.getButton());
        });
    }

    @Override
    public void sendMouseWheelEvent(MouseWheelEvent e) {
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
    public String toString() {
        return "CefRemoteBrowser_" + myBid;
    }
}
