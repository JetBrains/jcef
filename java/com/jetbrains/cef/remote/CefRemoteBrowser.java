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
import org.cef.misc.CefPdfPrintSettings;
import org.cef.network.CefRequest;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

public class CefRemoteBrowser implements CefBrowser {
    private final CefServer myServer;
    private final int myBid;

    public CefRemoteBrowser(CefServer owner, int bid) {
        this.myServer = owner;
        this.myBid = bid;
    }

    public int getBid() { return myBid; }

    @Override
    public void createImmediately() {

    }

    @Override
    public Component getUIComponent() {
        return null;
    }

    @Override
    public CefClient getClient() {
        return null;
    }

    @Override
    public CefRequestContext getRequestContext() {
        return null;
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return null;
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

    }

    @Override
    public void reloadIgnoreCache() {

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

    }

    @Override
    public void executeJavaScript(String code, String url, int line) {

    }

    @Override
    public String getURL() {
        return null;
    }

    @Override
    public void close(boolean force) {
        myServer.closeBrowser(myBid);
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
        if (myServer == null)
            return;

        int[] data = new int[]{width, height};
        ByteBuffer params = ByteBuffer.allocate(data.length*4);
        params.order(ByteOrder.nativeOrder());
        params.asIntBuffer().put(data);
        myServer.invoke(myBid, "wasresized", params);
    }

    @Override
    public void notifyScreenInfoChanged() {

    }

    @Override
    public void sendKeyEvent(KeyEvent e) {
        if (myServer == null)
            return;

        int[] data = new int[]{
                e.getID() == KeyEvent.KEY_RELEASED ? 0 : 1,
                e.getKeyChar(),
                e.getKeyCode(),
                e.getModifiersEx()
        };
        ByteBuffer params = ByteBuffer.allocate(data.length*4);
        params.order(ByteOrder.nativeOrder());
        params.asIntBuffer().put(data);
        myServer.invoke(myBid, "sendkeyevent", params);
    }

    @Override
    public void sendMouseEvent(MouseEvent e) {
        if (myServer == null)
            return;

        int[] data = new int[]{
                e.getID() == MouseEvent.MOUSE_RELEASED ? 0 : 1,
                e.getModifiersEx(),
                e.getX(),
                e.getY(),
                e.getXOnScreen(),
                e.getYOnScreen(),
                e.getClickCount(),
                e.isPopupTrigger() ? 1 : 0,
                e.getButton()
        };
        ByteBuffer params = ByteBuffer.allocate(data.length*4);
        params.order(ByteOrder.nativeOrder());
        params.asIntBuffer().put(data);
        myServer.invoke(myBid, "sendmouseevent", params);
    }

    @Override
    public void sendMouseWheelEvent(MouseWheelEvent e) {

    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        return null;
    }
}
