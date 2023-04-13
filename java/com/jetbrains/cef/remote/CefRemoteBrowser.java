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
import org.cef.misc.CefLog;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.network.CefRequest;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

public class CefRemoteBrowser implements CefBrowser {
    private static final Charset ourCharset = Charset.forName("UTF-8");
    private static final CharsetEncoder ourEncoder = ourCharset.newEncoder();
    private static final CharsetDecoder ourDecoder = ourCharset.newDecoder();

    private final CefServer myServer;
    private final int myBid;
    private final CefRemoteClient myOwner;

    public CefRemoteBrowser(CefServer server, int bid, CefRemoteClient owner) {
        this.myServer = server;
        this.myBid = bid;
        myOwner = owner;
        myOwner.registerBrowser(bid, this);
    }

    public int getBid() { return myBid; }
    public int getCid() { return myOwner.getCid(); }

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
        if (myServer == null)
            return;

        try {
            ByteBuffer params = ourEncoder.encode(CharBuffer.wrap(url));
            myServer.invoke(myBid, "loadurl", params);
        } catch (CharacterCodingException e) {
            CefLog.Error("loadURL can't encode string '%s', CharacterCodingException: %s", url, e.getMessage());
        }
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
        myServer.closeBrowser(myOwner.getCid(), myBid);
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
            e.getID(),
            e.getModifiersEx(),
            e.getKeyChar(),
            0, // TODO: get e.scancode via reflection (windows only)
            e.getKeyCode()
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

    @Override
    public String toString() {
        return "CefRemoteBrowser_" + myBid;
    }
}
