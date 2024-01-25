// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefDevToolsClient.DevToolsException;
import org.cef.callback.CefDragData;
import org.cef.callback.CefNativeAdapter;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.*;
import org.cef.handler.CefDialogHandler.FileDialogMode;
import org.cef.input.CefCompositionUnderline;
import org.cef.input.CefTouchEvent;
import org.cef.misc.CefLog;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.misc.CefRange;
import org.cef.network.CefRequest;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

import javax.swing.SwingUtilities;

/**
 * This class represents all methods which are connected to the
 * native counterpart CEF.
 * The visibility of this class is "package". To create a new
 * CefBrowser instance, please use CefBrowserFactory.
 */
abstract class CefBrowser_N extends CefNativeAdapter implements CefBrowser, CefAppStateHandler {
    private static final boolean TRACE_LIFESPAN = Boolean.getBoolean("jcef.trace.cefbrowser_n.lifespan");
    private volatile boolean isPending_ = false;
    private final CefClient client_;
    private final String url_;
    private CefRequestContext request_context_;
    private volatile CefBrowser_N parent_ = null;
    private volatile Point inspectAt_ = null;
    private volatile CefBrowser_N devTools_ = null;
    private volatile CefDevToolsClient devToolsClient_ = null;
    private boolean closeAllowed_ = false;
    private volatile boolean isClosed_ = false;
    private volatile boolean isClosing_ = false;
    private volatile boolean isCreating_ = false;

    private boolean isNativeCtxInitialized_ = false;
    private final List<Runnable> delayedActions_ = new ArrayList<>();

    protected CefBrowser_N(CefClient client, String url, CefRequestContext context,
            CefBrowser_N parent, Point inspectAt) {
        client_ = client;
        url_ = url;
        request_context_ = context;
        parent_ = parent;
        inspectAt_ = inspectAt;

        CefApp.getInstance().onInitialization(this);
    }

    protected String getUrl() {
        return url_;
    }

    protected CefBrowser_N getParentBrowser() {
        return parent_;
    }

    protected Point getInspectAt() {
        return inspectAt_;
    }

    @Override
    public void stateHasChanged(CefApp.CefAppState state) {
        if (isClosing_ || isClosed_)
            return;

        if (CefApp.CefAppState.INITIALIZED == state) {
            synchronized (delayedActions_) {
                isNativeCtxInitialized_ = true;
                if (request_context_ == null)
                    request_context_ = CefRequestContext.getGlobalContext();
                delayedActions_.forEach(r -> r.run());
                delayedActions_.clear();
            }
        }
    }

    @Override
    public boolean isClosed() {
        return isClosed_;
    }

    @Override
    public boolean isClosing() {
        return isClosing_;
    }

    @Override
    public CefClient getClient() {
        return client_;
    }

    @Override
    public CefRequestContext getRequestContext() {
        return request_context_;
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
    public synchronized void setCloseAllowed() {
        closeAllowed_ = true;
    }

    @Override
    public synchronized boolean doClose() {
        if (closeAllowed_) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // Trigger close of the parent window.
                    Component uiComponent = getUIComponent();
                    if (uiComponent == null) return;
                    Component parent = SwingUtilities.getRoot(uiComponent);
                    if (parent != null) {
                        parent.dispatchEvent(
                                new WindowEvent((Window) parent, WindowEvent.WINDOW_CLOSING));
                    }
                }
            });
            // Allow the close to proceed.
            return false;
        }
        // Cancel the close.
        return true;
    }

    @Override
    public synchronized void onBeforeClose() {
        if (TRACE_LIFESPAN) CefLog.Debug("CefBrowser_N: %s: onBeforeClose", this);
        isClosed_ = true;
        if (request_context_ != null) request_context_.dispose();
        if (parent_ != null) {
            parent_.closeDevTools();
            parent_.devTools_ = null;
            parent_ = null;
        }
        if (devToolsClient_ != null) {
            devToolsClient_.close();
        }
    }

    @Override
    public CefBrowser getDevTools() {
        return getDevTools(null);
    }

    @Override
    public synchronized CefBrowser getDevTools(Point inspectAt) {
        if (devTools_ == null) {
            devTools_ = (CefBrowser_N) createDevToolsBrowser(client_, url_, request_context_, this, inspectAt);
        }
        return devTools_;
    }

    protected abstract CefBrowser createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser parent, Point inspectAt);

    private void executeNative(Runnable nativeRunnable, String name) {
        synchronized (delayedActions_) {
            if (isNativeCtxInitialized_)
                nativeRunnable.run();
            else {
                CefLog.Debug("CefBrowser_N: %s: add delayed action %s", this, name);
                delayedActions_.add(nativeRunnable);
            }
        }
    }

    private void checkNativeCtxInitialized() {
        if (!isNativeCtxInitialized_) {
            String m1 = new Throwable().getStackTrace()[1].getMethodName();
            CefLog.Error("CefBrowser_N: %s: can't invoke native method '%s' before native context initialized", this, m1);
        }
    }
    @Override
    public synchronized CefDevToolsClient getDevToolsClient() {
        if (!isPending_ || isClosing_ || isClosed_) {
            return null;
        }
        if (devToolsClient_ == null || devToolsClient_.isClosed()) {
            devToolsClient_ = new CefDevToolsClient(this);
        }
        return devToolsClient_;
    }

    CompletableFuture<Integer> executeDevToolsMethod(String method, String parametersAsJson) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        N_ExecuteDevToolsMethod(method, parametersAsJson, new DevToolsMethodCallback() {
            @Override
            public void onComplete(int generatedMessageId) {
                if (generatedMessageId <= 0) {
                    future.completeExceptionally(new DevToolsException(
                            String.format("Failed to execute DevTools method %s", method)));
                } else {
                    future.complete(generatedMessageId);
                }
            }
        });
        return future;
    }

    CefRegistration addDevToolsMessageObserver(CefDevToolsMessageObserver observer) {
        return N_AddDevToolsMessageObserver(observer);
    }

    /**
     * Create a new browser.
     */
    protected void createBrowser(CefClientHandler clientHandler, long windowHandle, String url,
            boolean osr, boolean transparent, Component canvas) {
        if (isClosing_ || isClosed_ || isCreating_)
            return;

        if (getNativeRef("CefBrowser") == 0 && !isPending_) {
            isCreating_ = true;
            executeNative(() -> {
                try {
                    if (TRACE_LIFESPAN) CefLog.Debug("CefBrowser_N: %s: started native creation", this);
                    N_CreateBrowser(clientHandler, windowHandle, url, osr, transparent, canvas, request_context_);
                } catch (UnsatisfiedLinkError err) {
                    err.printStackTrace();
                }
            }, "createBrowser");
        }
    }

    /**
     * Called async from the (native) main UI thread.
     */
    private void notifyBrowserCreated() {
        isPending_ = true;
    }

    /**
     * Create a new browser as dev tools
     */
    protected final void createDevTools(CefBrowser_N parent, CefClientHandler clientHandler,
            long windowHandle, boolean osr, boolean transparent, Component canvas,
            Point inspectAt) {
        if (getNativeRef("CefBrowser") == 0 && !isPending_) {
            executeNative(() -> {
                try {
                    isPending_ = N_CreateDevTools(
                            parent, clientHandler, windowHandle, osr, transparent, canvas, inspectAt);
                } catch (UnsatisfiedLinkError err) {
                    err.printStackTrace();
                }
            }, "createDevTools");
        }
    }

    /**
     * Returns the native window handle for the specified native surface handle.
     */
    protected final long getWindowHandle(long surfaceHandle) {
        try {
            // NOTE: doesn't use cef ctx
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetWindowHandle(surfaceHandle);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
        return 0;
    }

    @Override
    protected void finalize() throws Throwable {
        close(true);
        super.finalize();
    }

    //
    // NOTE: all native methods checks native CefBrowser pointer at first,
    // so they won't use native context before native CefBrowser created
    //

    @Override
    public boolean canGoBack() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_CanGoBack();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void goBack() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_GoBack();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean canGoForward() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_CanGoForward();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void goForward() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_GoForward();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean isLoading() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_IsLoading();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void reload() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_Reload();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void reloadIgnoreCache() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_ReloadIgnoreCache();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void stopLoad() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_StopLoad();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public int getIdentifier() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetIdentifier();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return -1;
    }

    @Override
    public CefFrame getMainFrame() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetMainFrame();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public CefFrame getFocusedFrame() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetFocusedFrame();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public CefFrame getFrame(long identifier) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetFrame(identifier);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public CefFrame getFrame(String name) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetFrame2(name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public Vector<Long> getFrameIdentifiers() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetFrameIdentifiers();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public Vector<String> getFrameNames() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetFrameNames();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public int getFrameCount() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetFrameCount();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean isPopup() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_IsPopup();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean hasDocument() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_HasDocument();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void viewSource() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_ViewSource();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void getSource(CefStringVisitor visitor) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_GetSource(visitor);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void getText(CefStringVisitor visitor) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_GetText(visitor);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void loadRequest(CefRequest request) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_LoadRequest(request);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void loadURL(String url) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_LoadURL(url);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_ExecuteJavaScript(code, url, line);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String getURL() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetURL();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return "";
    }

    @Override
    public void close(boolean force) {
        if (isClosing_ || isClosed_) return;
        if (force) isClosing_ = true;

        if (TRACE_LIFESPAN) CefLog.Debug("CefBrowser_N: %s: close, force=%d", this.toString(), force ? 1 : 0);

        synchronized (delayedActions_) {
            delayedActions_.clear();
        }

        if (getNativeRef("CefBrowser") == 0) {
            CefLog.Debug("CefBrowser_N: %s: native part of browser wasn't created yet, browser will be closed immediately after creation", this);
            if (client_ != null) {
                client_.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                    @Override
                    public void onAfterCreated(CefBrowser browser) {
                        if (browser == CefBrowser_N.this) {
                            CefLog.Debug("CefBrowser_N: %s: close browser (immediately after creation)", browser);
                            browser.close(force);
                        }
                    }
                });
            }
            return;
        }

        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_Close(force);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void setFocus(boolean enable) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_SetFocus(enable);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void setWindowVisibility(boolean visible) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_SetWindowVisibility(visible);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public double getZoomLevel() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                return N_GetZoomLevel();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public void setZoomLevel(double zoomLevel) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_SetZoomLevel(zoomLevel);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void runFileDialog(FileDialogMode mode, String title, String defaultFilePath,
            Vector<String> acceptFilters, CefRunFileDialogCallback callback) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_RunFileDialog(
                    mode, title, defaultFilePath, acceptFilters, callback);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void startDownload(String url) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_StartDownload(url);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void print() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_Print();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void printToPDF(
            String path, CefPdfPrintSettings settings, CefPdfPrintCallback callback) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path was null or empty");
        }
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_PrintToPDF(path, settings, callback);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_Find(searchText, forward, matchCase, findNext);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void stopFinding(boolean clearSelection) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_StopFinding(clearSelection);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    protected final void closeDevTools() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_CloseDevTools();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void replaceMisspelling(String word) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_ReplaceMisspelling(word);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Notify that the browser was resized.
     * @param width The new width of the browser
     * @param height The new height of the browser
     */
    @Override
    public final void wasResized(int width, int height) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_WasResized(width, height);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void notifyScreenInfoChanged() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_NotifyScreenInfoChanged();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Invalidate the UI.
     */
    protected final void invalidate() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_Invalidate();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Send a key event.
     * @param e The event to send.
     */
    @Override
    public final void sendKeyEvent(KeyEvent e) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_SendKeyEvent(e);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Send a mouse event.
     * @param e The event to send.
     */
    @Override
    public final void sendMouseEvent(MouseEvent e) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_SendMouseEvent(e);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Send a mouse wheel event.
     * @param e The event to send.
     */
    @Override
    public final void sendMouseWheelEvent(MouseWheelEvent e) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_SendMouseWheelEvent(e);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void sendTouchEvent(CefTouchEvent e) {
        try {
            N_SendTouchEvent(e);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the user drags the mouse into the web view (before
     * calling DragTargetDragOver/DragTargetLeave/DragTargetDrop).
     * |drag_data| should not contain file contents as this type of data is not
     * allowed to be dragged into the web view. File contents can be removed using
     * CefDragData::ResetFileContents (for example, if |drag_data| comes from
     * CefRenderHandler::StartDragging).
     * This method is only used when window rendering is disabled.
     */
    protected final void dragTargetDragEnter(
            CefDragData dragData, Point pos, int modifiers, int allowedOps) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_DragTargetDragEnter(dragData, pos, modifiers, allowedOps);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method each time the mouse is moved across the web view during
     * a drag operation (after calling DragTargetDragEnter and before calling
     * DragTargetDragLeave/DragTargetDrop).
     * This method is only used when window rendering is disabled.
     */
    protected final void dragTargetDragOver(Point pos, int modifiers, int allowedOps) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_DragTargetDragOver(pos, modifiers, allowedOps);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the user drags the mouse out of the web view (after
     * calling DragTargetDragEnter).
     * This method is only used when window rendering is disabled.
     */
    protected final void dragTargetDragLeave() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_DragTargetDragLeave();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the user completes the drag operation by dropping
     * the object onto the web view (after calling DragTargetDragEnter).
     * The object being dropped is |drag_data|, given as an argument to
     * the previous DragTargetDragEnter call.
     * This method is only used when window rendering is disabled.
     */
    protected final void dragTargetDrop(Point pos, int modifiers) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_DragTargetDrop(pos, modifiers);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the drag operation started by a
     * CefRenderHandler.startDragging call has ended either in a drop or
     * by being cancelled. |x| and |y| are mouse coordinates relative to the
     * upper-left corner of the view. If the web view is both the drag source
     * and the drag target then all DragTarget* methods should be called before
     * DragSource* methods.
     * This method is only used when window rendering is disabled.
     */
    protected final void dragSourceEndedAt(Point pos, int operation) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_DragSourceEndedAt(pos, operation);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the drag operation started by a
     * CefRenderHandler.startDragging call has completed. This method may be
     * called immediately without first calling DragSourceEndedAt to cancel a
     * drag operation. If the web view is both the drag source and the drag
     * target then all DragTarget* methods should be called before DragSource*
     * methods.
     * This method is only used when window rendering is disabled.
     */
    protected final void dragSourceSystemDragEnded() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_DragSourceSystemDragEnded();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    protected final void updateUI(Rectangle contentRect, Rectangle browserRect) {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_UpdateUI(contentRect, browserRect);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    protected final void setParent(long windowHandle, Component canvas) {
        if (isClosing_ || isClosed_) return;

        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_SetParent(windowHandle, canvas);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method if the browser frame was moved.
     * This fixes positioning of select popups and dismissal on window move/resize.
     */
    protected final void notifyMoveOrResizeStarted() {
        try {
            checkNativeCtxInitialized();
            if (isNativeCtxInitialized_)
                N_NotifyMoveOrResizeStarted();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

   @Override
   public final void ImeSetComposition(String text, List<CefCompositionUnderline> underlines,
                                       CefRange replacementRange, CefRange selectionRange) {
       try {
           checkNativeCtxInitialized();
           if (isNativeCtxInitialized_)
               N_ImeSetComposition(text, underlines, replacementRange, selectionRange);
       } catch (UnsatisfiedLinkError ule) {
           ule.printStackTrace();
       }
   }

    @Override
   public final void ImeCommitText(String text, CefRange replacementRange, int relativeCursorPos) {
       try {
           checkNativeCtxInitialized();
           if (isNativeCtxInitialized_)
               N_ImeCommitText(text, replacementRange, relativeCursorPos);
       } catch (UnsatisfiedLinkError ule) {
           ule.printStackTrace();
       }
   }

    @Override
   public final void ImeFinishComposingText(boolean keepSelection) {
       try {
           checkNativeCtxInitialized();
           if (isNativeCtxInitialized_)
               N_ImeFinishComposingText(keepSelection);
       } catch (UnsatisfiedLinkError ule) {
           ule.printStackTrace();
       }
   }

    @Override
   public final void ImeCancelComposing() {
       try {
           checkNativeCtxInitialized();
           if (isNativeCtxInitialized_)
               N_ImeCancelComposing();
       } catch (UnsatisfiedLinkError ule) {
           ule.printStackTrace();
       }
   }

    private interface DevToolsMethodCallback {
        void onComplete(int generatedMessageId);
    }

    private final native boolean N_CreateBrowser(CefClientHandler clientHandler, long windowHandle,
            String url, boolean osr, boolean transparent, Component canvas,
            CefRequestContext context);
    private final native boolean N_CreateDevTools(CefBrowser parent, CefClientHandler clientHandler,
            long windowHandle, boolean osr, boolean transparent, Component canvas, Point inspectAt);
    private final native void N_ExecuteDevToolsMethod(
            String method, String parametersAsJson, DevToolsMethodCallback callback);
    private final native CefRegistration N_AddDevToolsMessageObserver(
            CefDevToolsMessageObserver observer);
    private final native long N_GetWindowHandle(long surfaceHandle);
    private final native boolean N_CanGoBack();
    private final native void N_GoBack();
    private final native boolean N_CanGoForward();
    private final native void N_GoForward();
    private final native boolean N_IsLoading();
    private final native void N_Reload();
    private final native void N_ReloadIgnoreCache();
    private final native void N_StopLoad();
    private final native int N_GetIdentifier();
    private final native CefFrame N_GetMainFrame();
    private final native CefFrame N_GetFocusedFrame();
    private final native CefFrame N_GetFrame(long identifier);
    private final native CefFrame N_GetFrame2(String name);
    private final native Vector<Long> N_GetFrameIdentifiers();
    private final native Vector<String> N_GetFrameNames();
    private final native int N_GetFrameCount();
    private final native boolean N_IsPopup();
    private final native boolean N_HasDocument();
    private final native void N_ViewSource();
    private final native void N_GetSource(CefStringVisitor visitor);
    private final native void N_GetText(CefStringVisitor visitor);
    private final native void N_LoadRequest(CefRequest request);
    private final native void N_LoadURL(String url);
    private final native void N_ExecuteJavaScript(String code, String url, int line);
    private final native String N_GetURL();
    private final native void N_Close(boolean force);
    private final native void N_SetFocus(boolean enable);
    private final native void N_SetWindowVisibility(boolean visible);
    private final native double N_GetZoomLevel();
    private final native void N_SetZoomLevel(double zoomLevel);
    private final native void N_RunFileDialog(FileDialogMode mode, String title,
            String defaultFilePath, Vector<String> acceptFilters, CefRunFileDialogCallback callback);
    private final native void N_StartDownload(String url);
    private final native void N_Print();
    private final native void N_PrintToPDF(
            String path, CefPdfPrintSettings settings, CefPdfPrintCallback callback);
    private final native void N_Find(
            String searchText, boolean forward, boolean matchCase, boolean findNext);
    private final native void N_StopFinding(boolean clearSelection);
    private final native void N_CloseDevTools();
    private final native void N_ReplaceMisspelling(String word);
    private final native void N_WasResized(int width, int height);
    private final native void N_Invalidate();
    private final native void N_NotifyScreenInfoChanged();
    private final native void N_SendKeyEvent(KeyEvent e);
    private final native void N_SendTouchEvent(CefTouchEvent e);
    private final native void N_SendMouseEvent(MouseEvent e);
    private final native void N_SendMouseWheelEvent(MouseWheelEvent e);
    private final native void N_DragTargetDragEnter(
            CefDragData dragData, Point pos, int modifiers, int allowed_ops);
    private final native void N_DragTargetDragOver(Point pos, int modifiers, int allowed_ops);
    private final native void N_DragTargetDragLeave();
    private final native void N_DragTargetDrop(Point pos, int modifiers);
    private final native void N_DragSourceEndedAt(Point pos, int operation);
    private final native void N_DragSourceSystemDragEnded();
    private final native void N_UpdateUI(Rectangle contentRect, Rectangle browserRect);
    private final native void N_SetParent(long windowHandle, Component canvas);
    private final native void N_NotifyMoveOrResizeStarted();
    private final native void N_ImeSetComposition(String text, List<CefCompositionUnderline> underlines,
                                                  CefRange replacementRange, CefRange selectionRange);
    private final native void N_ImeCommitText(String text, CefRange replacementRange, int relativeCursorPos);
    private final native void N_ImeFinishComposingText(boolean keepSelection);
    private final native void N_ImeCancelComposing();
}
