// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef;

import com.jetbrains.cef.remote.RemoteBrowser;
import org.cef.browser.*;
import com.jetbrains.cef.JCefAppConfig;
import com.jetbrains.cef.JdkEx;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RemoteClient;
import org.cef.callback.*;
import org.cef.handler.CefClientHandler;
import org.cef.handler.CefContextMenuHandler;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefDownloadHandler;
import org.cef.handler.CefDragHandler;
import org.cef.handler.CefFocusHandler;
import org.cef.handler.CefJSDialogHandler;
import org.cef.handler.CefKeyboardHandler;
import org.cef.handler.CefLifeSpanHandler;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefPrintHandler;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefRequestHandler;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefScreenInfo;
import org.cef.handler.CefWindowHandler;
import org.cef.handler.CefPermissionHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.CefPrintSettings;
import org.cef.misc.CefLog;
import org.cef.misc.CefRange;
import org.cef.network.CefRequest;
import org.cef.network.CefRequest.TransitionType;
import org.cef.security.CefSSLInfo;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Vector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Client that owns a browser and renderer.
 */
public class CefClient extends CefClientHandler
        implements CefContextMenuHandler, CefDialogHandler, CefDisplayHandler, CefDownloadHandler,
                   CefDragHandler, CefFocusHandler, CefPermissionHandler, CefJSDialogHandler, CefKeyboardHandler,
                   CefLifeSpanHandler, CefLoadHandler, CefPrintHandler, CefRenderHandler,
                   CefRequestHandler, CefWindowHandler {
    private static final boolean TRACE_LIFESPAN = Boolean.getBoolean("jcef.trace.cefclient.lifespan");
    // Delegate for remote implementation.
    private final RemoteClient remoteClient;

    // Fields for JNI implementation.
    private final ConcurrentHashMap<Integer, CefBrowser> browser_ = new ConcurrentHashMap<Integer, CefBrowser>();
    private CefContextMenuHandler contextMenuHandler_ = null;
    private CefDialogHandler dialogHandler_ = null;
    private CefDisplayHandler displayHandler_ = null;
    private CefDownloadHandler downloadHandler_ = null;
    private CefDragHandler dragHandler_ = null;
    private CefFocusHandler focusHandler_ = null;
    private CefPermissionHandler permissionHandler_ = null;
    private CefJSDialogHandler jsDialogHandler_ = null;
    private CefKeyboardHandler keyboardHandler_ = null;
    private final List<CefLifeSpanHandler> lifeSpanHandlers_ = new ArrayList<>();
    private CefLoadHandler loadHandler_ = null;
    private CefPrintHandler printHandler_ = null;
    private CefRequestHandler requestHandler_ = null;
    private boolean isDisposed_ = false;
    private Runnable onDisposed_ = null; // just for convenience (for tests debugging)
    private volatile CefBrowser focusedBrowser_ = null;
    private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (focusedBrowser_ != null) {
                Component browserUI = focusedBrowser_.getUIComponent();
                if (browserUI == null) return;
                Object oldUI = evt.getOldValue();
                if (isPartOf(oldUI, browserUI)) {
                    focusedBrowser_.setFocus(false);
                    focusedBrowser_ = null;
                }
            }
        }
    };

    /**
     * The CTOR is only accessible within this package.
     * Use CefApp.createClient() to create an instance of
     * this class.
     *
     * @see org.cef.CefApp#createClient()
     */
    CefClient() throws UnsatisfiedLinkError {
        super();

        remoteClient = CefApp.isRemoteEnabled() ? CefServer.instance().createClient() : null;
        if (remoteClient == null)
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(propertyChangeListener);

        if (TRACE_LIFESPAN) CefLog.Debug("CefClient: created client %s [remote=%s]", this, remoteClient);
    }

    private boolean isPartOf(Object obj, Component browserUI) {
        if (obj == browserUI) return true;
        if (obj instanceof Container) {
            Component childs[] = ((Container) obj).getComponents();
            for (Component child : childs) {
                return isPartOf(child, browserUI);
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        if (TRACE_LIFESPAN) CefLog.Debug("CefClient: dispose client %s [remote=%s]", this, remoteClient);
        isDisposed_ = true;
        if (remoteClient != null) {
            // NOTE: super.dispose() shouldn't be called here
            remoteClient.dispose();
            CefApp app = CefApp.getInstanceIfAny();
            if (app != null) app.clientWasDisposed(this);
            if (onDisposed_ != null) onDisposed_.run();
        } else
            cleanupBrowser(-1);
    }

    public void setOnDisposeCallback(Runnable onDisposed) {
        this.onDisposed_ = onDisposed;
    }
    // CefClientHandler

    /**
     * @deprecated {@link #createBrowser(String, CefRendering, boolean)}
     */
    @Deprecated
    public CefBrowser createBrowser(
            String url, boolean isOffscreenRendered, boolean isTransparent) {
        return createBrowser(url, isOffscreenRendered, isTransparent, null);
    }


    /**
     * @deprecated {@link #createBrowser(String, CefRendering, boolean, CefRequestContext)}
     */
    @Deprecated
    public CefBrowser createBrowser(String url, boolean isOffscreenRendered, boolean isTransparent,
                                    CefRequestContext context) {
        return createBrowser(url, isOffscreenRendered ? CefRendering.OFFSCREEN : CefRendering.DEFAULT, isTransparent, context);
    }

    @Deprecated
    public CefBrowser createBrowser(String url, boolean isOffscreenRendered, boolean isTransparent,
                                    CefRequestContext context, CefBrowserSettings settings) {
        return createBrowser(url, isOffscreenRendered ? CefRendering.OFFSCREEN : CefRendering.DEFAULT, isTransparent, context, settings);
    }

    public CefBrowser createBrowser(String url, CefRendering rendering, boolean isTransparent) {
        return createBrowser(url, rendering, isTransparent, null);
    }

    public CefBrowser createBrowser(String url, CefRendering rendering, boolean isTransparent,
                                    CefRequestContext context) {
        if (isDisposed_)
            throw new IllegalStateException("Can't create browser. CefClient is disposed");
        if (remoteClient != null)
            return remoteClient.createBrowser(url, context, this, rendering);
        return CefBrowserFactory.create(this, url, rendering, isTransparent, context, null);
    }

    public CefBrowser createBrowser(String url, CefRendering rendering, boolean isTransparent,
                                    CefRequestContext context, CefBrowserSettings settings) {
        if (isDisposed_)
            throw new IllegalStateException("Can't create browser. CefClient is disposed");
        // TODO: add CefBrowserSettings to RemoteClient#createBrowser
        if (remoteClient != null)
            return remoteClient.createBrowser(url, context, this, rendering);
        return CefBrowserFactory.create(
                this, url, rendering, isTransparent, context, settings);
    }

    @Override
    protected CefBrowser getBrowser(int identifier) {
        if (remoteClient != null)
            return remoteClient.getRemoteBrowser(identifier);
        return browser_.get(identifier);
    }

    @Override
    protected Object[] getAllBrowser() {
        if (remoteClient != null)
            return remoteClient.getAllBrowsers();
        return browser_.values().stream().filter(browser -> !browser.isClosing()).toArray();
    }

    @Override
    protected CefContextMenuHandler getContextMenuHandler() {
        if (remoteClient != null)
            return remoteClient.getContextMenuHandler();
        return this;
    }

    @Override
    protected CefDialogHandler getDialogHandler() {
        if (remoteClient != null)
            return remoteClient.getDialogHandler();
        return this;
    }

    @Override
    protected CefDisplayHandler getDisplayHandler() {
        if (remoteClient != null)
            return remoteClient.getDisplayHandler();
        return this;
    }

    @Override
    protected CefDownloadHandler getDownloadHandler() {
        if (remoteClient != null)
            return remoteClient.getDownloadHandler();
        return this;
    }

    @Override
    protected CefDragHandler getDragHandler() {
        if (remoteClient != null)
            return remoteClient.getDragHandler();
        return this;
    }

    @Override
    protected CefFocusHandler getFocusHandler() {
        if (remoteClient != null)
            return remoteClient.getFocusHandler();
        return this;
    }

    @Override
    protected CefPermissionHandler getPermissionHandler() {
        if (remoteClient != null)
            return remoteClient.getPermissionHandler();
        return this;
    }

    @Override
    protected CefJSDialogHandler getJSDialogHandler() {
        if (remoteClient != null)
            return remoteClient.getJSDialogHandler();
        return this;
    }

    @Override
    protected CefKeyboardHandler getKeyboardHandler() {
        if (remoteClient != null)
            return remoteClient.getKeyboardHandler();
        return this;
    }

    @Override
    protected CefLifeSpanHandler getLifeSpanHandler() {
        if (remoteClient != null) {
            CefLog.Error("CefClient.getLifeSpanHandler mustn't be called in remote mode.");
            return null;
        }
        return this;
    }

    @Override
    protected CefLoadHandler getLoadHandler() {
        if (remoteClient != null)
            return remoteClient.getLoadHandler();
        return this;
    }

    @Override
    protected CefPrintHandler getPrintHandler() {
        if (remoteClient != null)
            return remoteClient.getPrintHandler();
        return this;
    }

    @Override
    protected CefRenderHandler getRenderHandler() {
        if (remoteClient != null) {
            CefLog.Error("CefClient.getRenderHandler mustn't be called in remote mode.");
            return null;
        }
        return this;
    }

    @Override
    protected CefRequestHandler getRequestHandler() {
        if (remoteClient != null)
            return remoteClient.getRequestHandler();
        return this;
    }

    @Override
    protected CefWindowHandler getWindowHandler() {
        if (remoteClient != null)
            return null; // only OSR rendering in remote mode
        return this;
    }

    // CefContextMenuHandler

    public CefClient addContextMenuHandler(CefContextMenuHandler handler) {
        if (remoteClient != null)
            remoteClient.addContextMenuHandler(handler);
        else
            if (contextMenuHandler_ == null) contextMenuHandler_ = handler;
        return this;
    }

    public void removeContextMenuHandler() {
        if (remoteClient != null)
            remoteClient.removeContextMenuHandler();
        else
            contextMenuHandler_ = null;
    }

    @Override
    public void onBeforeContextMenu(
            CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
        if (remoteClient != null) CefLog.Error("onBeforeContextMenu mustn't be called in remote mode (it seems that user manually called this method).");
        if (contextMenuHandler_ != null && browser != null)
            contextMenuHandler_.onBeforeContextMenu(browser, frame, params, model);
    }

    @Override
    public boolean onContextMenuCommand(CefBrowser browser, CefFrame frame,
                                        CefContextMenuParams params, int commandId, int eventFlags) {
        if (remoteClient != null) CefLog.Error("onContextMenuCommand mustn't be called in remote mode (it seems that user manually called this method).");
        if (contextMenuHandler_ != null && browser != null)
            return contextMenuHandler_.onContextMenuCommand(
                    browser, frame, params, commandId, eventFlags);
        return false;
    }

    @Override
    public void onContextMenuDismissed(CefBrowser browser, CefFrame frame) {
        if (remoteClient != null) CefLog.Error("onContextMenuDismissed mustn't be called in remote mode (it seems that user manually called this method).");
        if (contextMenuHandler_ != null && browser != null)
            contextMenuHandler_.onContextMenuDismissed(browser, frame);
    }

    // CefDialogHandler

    public CefClient addDialogHandler(CefDialogHandler handler) {
        if (remoteClient != null)
            remoteClient.addDialogHandler(handler);
        else
            if (dialogHandler_ == null) dialogHandler_ = handler;
        return this;
    }

    public void removeDialogHandler() {
        if (remoteClient != null)
            remoteClient.removeDialogHandler();
        else
            dialogHandler_ = null;
    }

    @Override
    public boolean onFileDialog(CefBrowser browser, FileDialogMode mode, String title,
                                String defaultFilePath, Vector<String> acceptFilters, CefFileDialogCallback callback) {
        if (remoteClient != null) CefLog.Error("onFileDialog mustn't be called in remote mode (it seems that user manually called this method).");
        if (dialogHandler_ != null && browser != null) {
            return dialogHandler_.onFileDialog(
                    browser, mode, title, defaultFilePath, acceptFilters, callback);
        }
        return false;
    }

    // CefDisplayHandler

    public CefClient addDisplayHandler(CefDisplayHandler handler) {
        if (remoteClient != null)
            remoteClient.addDisplayHandler(handler);
        else
            if (displayHandler_ == null) displayHandler_ = handler;
        return this;
    }

    public void removeDisplayHandler() {
        if (remoteClient != null)
            remoteClient.removeDisplayHandler();
        else
            displayHandler_ = null;
    }

    @Override
    public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
        if (remoteClient != null) CefLog.Error("onAddressChange mustn't be called in remote mode (it seems that user manually called this method).");
        if (displayHandler_ != null && browser != null)
            displayHandler_.onAddressChange(browser, frame, url);
    }

    @Override
    public void onTitleChange(CefBrowser browser, String title) {
        if (remoteClient != null) CefLog.Error("onTitleChange mustn't be called in remote mode (it seems that user manually called this method).");
        if (displayHandler_ != null && browser != null)
            displayHandler_.onTitleChange(browser, title);
    }

    @Override
    public void onFullscreenModeChange(CefBrowser browser, boolean fullscreen) {
        if (displayHandler_ != null && browser != null)
            displayHandler_.onFullscreenModeChange(browser, fullscreen);
    }

    @Override
    public boolean onTooltip(CefBrowser browser, String text) {
        if (remoteClient != null) CefLog.Error("onTooltip mustn't be called in remote mode (it seems that user manually called this method).");
        if (displayHandler_ != null && browser != null) {
            return displayHandler_.onTooltip(browser, text);
        }
        return false;
    }

    @Override
    public void onStatusMessage(CefBrowser browser, String value) {
        if (remoteClient != null) CefLog.Error("onStatusMessage mustn't be called in remote mode (it seems that user manually called this method).");
        if (displayHandler_ != null && browser != null) {
            displayHandler_.onStatusMessage(browser, value);
        }
    }

    @Override
    public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level,
                                    String message, String source, int line) {
        if (remoteClient != null) CefLog.Error("onConsoleMessage mustn't be called in remote mode (it seems that user manually called this method).");
        if (displayHandler_ != null && browser != null) {
            return displayHandler_.onConsoleMessage(browser, level, message, source, line);
        }
        return false;
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        if (remoteClient != null) CefLog.Error("onCursorChange mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) {
            return false;
        }

        if (displayHandler_ != null && displayHandler_.onCursorChange(browser, cursorType)) {
            return true;
        }

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) {
            return realHandler.onCursorChange(browser, cursorType);
        }

        return false;
    }

    // CefDownloadHandler

    public CefClient addDownloadHandler(CefDownloadHandler handler) {
        if (remoteClient != null)
            remoteClient.addDownloadHandler(handler);
        else
            if (downloadHandler_ == null) downloadHandler_ = handler;
        return this;
    }

    public void removeDownloadHandler() {
        if (remoteClient != null)
            remoteClient.removeDownloadHandler();
        else
            downloadHandler_ = null;
    }

    @Override
    public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem,
                                 String suggestedName, CefBeforeDownloadCallback callback) {
        if (remoteClient != null) CefLog.Error("onBeforeDownload mustn't be called in remote mode (it seems that user manually called this method).");
        if (downloadHandler_ != null && browser != null)
            downloadHandler_.onBeforeDownload(browser, downloadItem, suggestedName, callback);
    }

    @Override
    public void onDownloadUpdated(
            CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
        if (remoteClient != null) CefLog.Error("onDownloadUpdated mustn't be called in remote mode (it seems that user manually called this method).");
        if (downloadHandler_ != null && browser != null)
            downloadHandler_.onDownloadUpdated(browser, downloadItem, callback);
    }

    // CefDragHandler

    public CefClient addDragHandler(CefDragHandler handler) {
        if (remoteClient != null)
            remoteClient.addDragHandler(handler);
        else
            if (dragHandler_ == null) dragHandler_ = handler;
        return this;
    }

    public void removeDragHandler() {
        if (remoteClient != null)
            remoteClient.removeDragHandler();
        else
            dragHandler_ = null;
    }

    @Override
    public boolean onDragEnter(CefBrowser browser, CefDragData dragData, int mask) {
        if (remoteClient != null) CefLog.Error("onDragEnter mustn't be called in remote mode (it seems that user manually called this method).");
        if (dragHandler_ != null && browser != null)
            return dragHandler_.onDragEnter(browser, dragData, mask);
        return false;
    }

    // CefFocusHandler

    public CefClient addFocusHandler(CefFocusHandler handler) {
        if (remoteClient != null)
            remoteClient.addFocusHandler(handler);
        else
            if (focusHandler_ == null) focusHandler_ = handler;
        return this;
    }

    public void removeFocusHandler() {
        if (remoteClient != null)
            remoteClient.removeFocusHandler();
        else
            focusHandler_ = null;
    }

    @Override
    public void onTakeFocus(CefBrowser browser, boolean next) {
        if (remoteClient != null) CefLog.Error("onTakeFocus mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;

        browser.setFocus(false);
        Component uiComponent = browser.getUIComponent();
        if (uiComponent == null) return;
        Container parent = uiComponent.getParent();
        if (parent != null) {
            FocusTraversalPolicy policy = null;
            while (parent != null) {
                policy = parent.getFocusTraversalPolicy();
                if (policy != null) break;
                parent = parent.getParent();
            }
            if (policy != null) {
                Component nextComp = next
                        ? policy.getComponentAfter(parent, uiComponent)
                        : policy.getComponentBefore(parent, uiComponent);
                if (nextComp == null) {
                    policy.getDefaultComponent(parent).requestFocus();
                } else {
                    nextComp.requestFocus();
                }
            }
        }
        focusedBrowser_ = null;
        if (focusHandler_ != null) focusHandler_.onTakeFocus(browser, next);
    }

    @Override
    public boolean onSetFocus(final CefBrowser browser, FocusSource source) {
        if (remoteClient != null) CefLog.Error("onSetFocus mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return false;

        Boolean alreadyHandled = Boolean.FALSE;
        if (focusHandler_ != null) {
            Component uiComponent = browser.getUIComponent();
            if (uiComponent == null) return true;
            alreadyHandled = JdkEx.invokeOnEDTAndWait(() -> focusHandler_.onSetFocus(browser, source), Boolean.TRUE /*ignore focus*/, uiComponent);
        }
        return alreadyHandled;
    }

    @Override
    public void onGotFocus(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("onGotFocus mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;
        if (focusedBrowser_ == browser) return; // prevent recursive call (in OSR)

        focusedBrowser_ = browser;
        browser.setFocus(true);
        if (focusHandler_ != null) {
            Component uiComponent = browser.getUIComponent();
            if (uiComponent == null) return;
            JdkEx.invokeOnEDTAndWait(() -> focusHandler_.onGotFocus(browser), uiComponent);
        }
    }

    // CefPermissionHandler

    public CefClient addPermissionHandler(CefPermissionHandler handler) {
        if (remoteClient != null)
            remoteClient.addPermissionHandler(handler);
        else
            if (permissionHandler_ == null) permissionHandler_ = handler;
        return this;
    }

    public void removePermissionHandler() {
        if (remoteClient != null)
            remoteClient.removePermissionHandler();
        else
            permissionHandler_ = null;
    }

    @Override
    public boolean onRequestMediaAccessPermission(
            CefBrowser browser,
            CefFrame frame,
            String requesting_url,
            int requested_permissions,
            CefMediaAccessCallback callback) {
        if (remoteClient != null) CefLog.Error("onRequestMediaAccessPermission mustn't be called in remote mode (it seems that user manually called this method).");
        if (permissionHandler_ != null && browser != null)
            return permissionHandler_.onRequestMediaAccessPermission(browser, frame, requesting_url,
                    requested_permissions, callback);
        return false;
    }

    // CefJSDialogHandler

    public CefClient addJSDialogHandler(CefJSDialogHandler handler) {
        if (remoteClient != null)
            remoteClient.addJSDialogHandler(handler);
        else
            if (jsDialogHandler_ == null) jsDialogHandler_ = handler;
        return this;
    }

    public void removeJSDialogHandler() {
        if (remoteClient != null)
            remoteClient.removeJSDialogHandler();
        else
            jsDialogHandler_ = null;
    }

    @Override
    public boolean onJSDialog(CefBrowser browser, String origin_url, JSDialogType dialog_type,
                              String message_text, String default_prompt_text, CefJSDialogCallback callback,
                              BoolRef suppress_message) {
        if (remoteClient != null) CefLog.Error("onJSDialog mustn't be called in remote mode (it seems that user manually called this method).");
        if (jsDialogHandler_ != null && browser != null)
            return jsDialogHandler_.onJSDialog(browser, origin_url, dialog_type, message_text,
                    default_prompt_text, callback, suppress_message);
        return false;
    }

    @Override
    public boolean onBeforeUnloadDialog(CefBrowser browser, String message_text, boolean is_reload,
                                        CefJSDialogCallback callback) {
        if (remoteClient != null) CefLog.Error("onBeforeUnloadDialog mustn't be called in remote mode (it seems that user manually called this method).");
        if (jsDialogHandler_ != null && browser != null)
            return jsDialogHandler_.onBeforeUnloadDialog(
                    browser, message_text, is_reload, callback);
        return false;
    }

    @Override
    public void onResetDialogState(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("onResetDialogState mustn't be called in remote mode (it seems that user manually called this method).");
        if (jsDialogHandler_ != null && browser != null)
            jsDialogHandler_.onResetDialogState(browser);
    }

    @Override
    public void onDialogClosed(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("onDialogClosed mustn't be called in remote mode (it seems that user manually called this method).");
        if (jsDialogHandler_ != null && browser != null) jsDialogHandler_.onDialogClosed(browser);
    }

    // CefKeyboardHandler

    public CefClient addKeyboardHandler(CefKeyboardHandler handler) {
        if (remoteClient != null)
            remoteClient.addKeyboardHandler(handler);
        else
            if (keyboardHandler_ == null) keyboardHandler_ = handler;
        return this;
    }

    public void removeKeyboardHandler() {
        if (remoteClient != null)
            remoteClient.removeKeyboardHandler();
        else
            keyboardHandler_ = null;
    }

    @Override
    public boolean onPreKeyEvent(
            CefBrowser browser, CefKeyEvent event, BoolRef is_keyboard_shortcut) {
        if (remoteClient != null) CefLog.Error("onPreKeyEvent mustn't be called in remote mode (it seems that user manually called this method).");
        if (keyboardHandler_ != null && browser != null)
            return keyboardHandler_.onPreKeyEvent(browser, event, is_keyboard_shortcut);
        return false;
    }

    @Override
    public boolean onKeyEvent(CefBrowser browser, CefKeyEvent event) {
        if (remoteClient != null) CefLog.Error("onKeyEvent mustn't be called in remote mode (it seems that user manually called this method).");
        if (keyboardHandler_ != null && browser != null)
            return keyboardHandler_.onKeyEvent(browser, event);
        return false;
    }

    // CefLifeSpanHandler

    public CefClient addLifeSpanHandler(CefLifeSpanHandler handler) {
        if (remoteClient != null)
            remoteClient.addLifeSpanHandler(handler);
        else
            synchronized (lifeSpanHandlers_) {
                lifeSpanHandlers_.add(handler);
            }
        return this;
    }

    public void removeLifeSpanHandler() {
        if (remoteClient != null)
            remoteClient.removeAllLifeSpanHandlers();
        else
            synchronized (lifeSpanHandlers_) {
                lifeSpanHandlers_.clear();
            }
    }

    @Override
    public boolean onBeforePopup(
            CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
        if (remoteClient != null) CefLog.Error("onBeforePopup mustn't be called in remote mode (it seems that user manually called this method).");
        if (isDisposed_) return true;
        if (browser == null)
            return false;
        synchronized (lifeSpanHandlers_) {
            boolean result = false;
            for (CefLifeSpanHandler lsh : lifeSpanHandlers_) {
                result |= lsh.onBeforePopup(browser, frame, target_url, target_frame_name);
            }
            return result;
        }
    }

    @Override
    public void onAfterCreated(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("onAfterCreated mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;
        if (TRACE_LIFESPAN) CefLog.Debug("CefClient: browser=%s: onAfterCreated", browser);
        boolean disposed = isDisposed_;

        if (disposed) CefLog.Info("Browser %s was created while CefClient was marked as disposed", browser);

        // keep browser reference
        Integer identifier = browser.getIdentifier();
        if (!disposed) {
            browser_.put(identifier, browser);
        }
        synchronized (lifeSpanHandlers_) {
            for (CefLifeSpanHandler lsh : lifeSpanHandlers_)
                lsh.onAfterCreated(browser);
        }
        if (disposed) {
            // Not sure, but it makes sense to close browser since it's not in browser_
            browser.close(true);
        }
    }

    @Override
    public void onAfterParentChanged(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("onAfterParentChanged mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;
        synchronized (lifeSpanHandlers_) {
            for (CefLifeSpanHandler lsh : lifeSpanHandlers_)
                lsh.onAfterParentChanged(browser);
        }
    }

    @Override
    public boolean doClose(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("doClose mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return false;
        synchronized (lifeSpanHandlers_) {
            for (CefLifeSpanHandler lsh : lifeSpanHandlers_)
                lsh.doClose(browser);
        }
        return browser.doClose();
    }

    @Override
    public void onBeforeClose(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("onBeforeClose mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;
        if (TRACE_LIFESPAN) CefLog.Debug("CefClient: browser=%s: onBeforeClose", browser);
        synchronized (lifeSpanHandlers_) {
            for (CefLifeSpanHandler lsh : lifeSpanHandlers_)
                lsh.onBeforeClose(browser);
        }
        browser.onBeforeClose();

        // remove browser reference
        cleanupBrowser(browser.getIdentifier());
    }

    private void cleanupBrowser(int identifier) {
        if (identifier >= 0) {
            // Remove the specific browser that closed.
            browser_.remove(identifier);
        } else {
            assert isDisposed_;
            Collection<CefBrowser> browserList = new ArrayList<>(browser_.values());
            if (!browserList.isEmpty()) {
                if (TRACE_LIFESPAN) CefLog.Debug("CefClient: cleanup %d browsers", browserList.size());
                // Close all browsers.
                // Once any of browsers close, it will invoke #onBeforeClose and #cleanupBrowser
                for (CefBrowser browser : browserList) {
                    if (TRACE_LIFESPAN) CefLog.Debug("CefClient: close %s", browser);
                    browser.close(true);
                }
                return;
            }
        }
        if (!isDisposed_) return;
        if (!browser_.isEmpty()) return;

        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(
                propertyChangeListener);
        removeContextMenuHandler(this);
        removeDialogHandler(this);
        removeDisplayHandler(this);
        removeDownloadHandler(this);
        removeDragHandler(this);
        removeFocusHandler(this);
        removeJSDialogHandler(this);
        removeKeyboardHandler(this);
        removeLifeSpanHandler(this);
        removeLoadHandler(this);
        removePrintHandler(this);
        removeRenderHandler(this);
        removeRequestHandler(this);
        removeWindowHandler(this);
        super.dispose();

        CefApp app = CefApp.getInstanceIfAny();
        if (app != null) app.clientWasDisposed(this);
        if (onDisposed_ != null) onDisposed_.run();
    }

    // CefLoadHandler

    public CefClient addLoadHandler(CefLoadHandler handler) {
        if (remoteClient != null)
            remoteClient.addLoadHandler(handler);
        else
            if (loadHandler_ == null) loadHandler_ = handler;
        return this;
    }

    public void removeLoadHandler() {
        if (remoteClient != null)
            remoteClient.removeLoadHandler();
        else
            loadHandler_ = null;
    }

    @Override
    public void onLoadingStateChange(
            CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        if (remoteClient != null) CefLog.Error("onLoadingStateChange mustn't be called in remote mode (it seems that user manually called this method).");
        if (loadHandler_ != null && browser != null)
            loadHandler_.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
    }

    @Override
    public void onLoadStart(CefBrowser browser, CefFrame frame, TransitionType transitionType) {
        if (remoteClient != null) CefLog.Error("onLoadStart mustn't be called in remote mode (it seems that user manually called this method).");
        if (loadHandler_ != null && browser != null)
            loadHandler_.onLoadStart(browser, frame, transitionType);
    }

    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        if (remoteClient != null) CefLog.Error("onLoadEnd mustn't be called in remote mode (it seems that user manually called this method).");
        if (loadHandler_ != null && browser != null)
            loadHandler_.onLoadEnd(browser, frame, httpStatusCode);
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode,
                            String errorText, String failedUrl) {
        if (remoteClient != null) CefLog.Error("onLoadError mustn't be called in remote mode (it seems that user manually called this method).");
        if (loadHandler_ != null && browser != null)
            loadHandler_.onLoadError(browser, frame, errorCode, errorText, failedUrl);
    }

    // CefPrintHandler

    public CefClient addPrintHandler(CefPrintHandler handler) {
        if (remoteClient != null)
            remoteClient.addPrintHandler(handler);
        else
            if (printHandler_ == null) printHandler_ = handler;
        return this;
    }

    public void removePrintHandler() {
        if (remoteClient != null)
            remoteClient.removePrintHandler();
        else
            printHandler_ = null;
    }

    @Override
    public void onPrintStart(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("onPrintStart mustn't be called in remote mode (it seems that user manually called this method).");
        if (printHandler_ != null && browser != null) printHandler_.onPrintStart(browser);
    }

    @Override
    public void onPrintSettings(
            CefBrowser browser, CefPrintSettings settings, boolean getDefaults) {
        if (remoteClient != null) CefLog.Error("onPrintSettings mustn't be called in remote mode (it seems that user manually called this method).");
        if (printHandler_ != null && browser != null)
            printHandler_.onPrintSettings(browser, settings, getDefaults);
    }

    @Override
    public boolean onPrintDialog(
            CefBrowser browser, boolean hasSelection, CefPrintDialogCallback callback) {
        if (remoteClient != null) CefLog.Error("onPrintDialog mustn't be called in remote mode (it seems that user manually called this method).");
        if (printHandler_ != null && browser != null)
            return printHandler_.onPrintDialog(browser, hasSelection, callback);
        return false;
    }

    @Override
    public boolean onPrintJob(CefBrowser browser, String documentName, String pdfFilePath,
                              CefPrintJobCallback callback) {
        if (remoteClient != null) CefLog.Error("onPrintJob mustn't be called in remote mode (it seems that user manually called this method).");
        if (printHandler_ != null && browser != null)
            return printHandler_.onPrintJob(browser, documentName, pdfFilePath, callback);
        return false;
    }

    @Override
    public void onPrintReset(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("onPrintReset mustn't be called in remote mode (it seems that user manually called this method).");
        if (printHandler_ != null && browser != null) printHandler_.onPrintReset(browser);
    }

    @Override
    public Dimension getPdfPaperSize(CefBrowser browser, int deviceUnitsPerInch) {
        if (remoteClient != null) CefLog.Error("getPdfPaperSize mustn't be called in remote mode (it seems that user manually called this method).");
        if (printHandler_ != null && browser != null)
            return printHandler_.getPdfPaperSize(browser, deviceUnitsPerInch);
        return null;
    }

    // CefMessageRouter

    @Override
    public synchronized void addMessageRouter(CefMessageRouter messageRouter) {
        if (remoteClient != null)
            remoteClient.addMessageRouter(messageRouter);
        else
            super.addMessageRouter(messageRouter);
    }

    @Override
    public synchronized void removeMessageRouter(CefMessageRouter messageRouter) {
        if (remoteClient != null)
            remoteClient.removeMessageRouter(messageRouter);
        else
            super.removeMessageRouter(messageRouter);
    }

    // CefRenderHandler

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("getViewRect mustn't be called in remote mode (it seems that user manually called this method).");
        // [tav] resize to 1x1 size to avoid crash in cef
        if (browser == null) return new Rectangle(0, 0, 1, 1);

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) {
            Rectangle rect = realHandler.getViewRect(browser);
            if (rect.width <= 0 || rect.height <= 0) {
                rect = new Rectangle(rect.x, rect.y, 1, 1);
            }
            return rect;
        }
        return new Rectangle(0, 0, 1, 1);
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        if (remoteClient != null) CefLog.Error("getScreenPoint mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return new Point(0, 0);

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) return realHandler.getScreenPoint(browser, viewPoint);
        return new Point(0, 0);
    }

    @Override
    public double getDeviceScaleFactor(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("getDeviceScaleFactor mustn't be called in remote mode (it seems that user manually called this method).");
        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) {
            return realHandler.getDeviceScaleFactor(browser);
        }
        return JCefAppConfig.getDeviceScaleFactor(browser.getUIComponent());
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        if (remoteClient != null) CefLog.Error("onPopupShow mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) realHandler.onPopupShow(browser, show);
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        if (remoteClient != null) CefLog.Error("onPopupSize mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) realHandler.onPopupSize(browser, size);
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                        ByteBuffer buffer, int width, int height) {
        if (remoteClient != null) CefLog.Error("onPaint mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null)
            realHandler.onPaint(browser, popup, dirtyRects, buffer, width, height);
    }

    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> listener) {}

    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> listener) {}

    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {}

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        if (remoteClient != null) CefLog.Error("startDragging mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return false;

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) return realHandler.startDragging(browser, dragData, mask, x, y);
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
        if (remoteClient != null) CefLog.Error("updateDragCursor mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) realHandler.updateDragCursor(browser, operation);
    }

    @Override
    public void OnImeCompositionRangeChanged(CefBrowser browser, CefRange selectionRange, Rectangle[] characterBounds) {
        if (remoteClient != null) CefLog.Error("OnImeCompositionRangeChanged mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;
        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) realHandler.OnImeCompositionRangeChanged(browser, selectionRange, characterBounds);
    }

    @Override
    public void OnTextSelectionChanged(CefBrowser browser, String selectedText, CefRange selectionRange) {
        if (remoteClient != null) CefLog.Error("OnTextSelectionChanged mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;
        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) realHandler.OnTextSelectionChanged(browser, selectedText, selectionRange);
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        if (remoteClient != null) CefLog.Error("getScreenInfo mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return false;

        CefRenderHandler realHandler = browser.getRenderHandler();
        if (realHandler != null) return realHandler.getScreenInfo(browser, screenInfo);
        return false;
    }

    // CefRequestHandler

    public CefClient addRequestHandler(CefRequestHandler handler) {
        if (remoteClient != null)
            remoteClient.addRequestHandler(handler);
        else
            if (requestHandler_ == null) requestHandler_ = handler;
        return this;
    }

    public void removeRequestHandler() {
        if (remoteClient != null)
            remoteClient.removeRequestHandler();
        else
            requestHandler_ = null;
    }

    @Override
    public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request,
                                  boolean user_gesture, boolean is_redirect) {
        if (remoteClient != null) CefLog.Error("onBeforeBrowse mustn't be called in remote mode (it seems that user manually called this method).");
        if (requestHandler_ != null && browser != null)
            return requestHandler_.onBeforeBrowse(
                    browser, frame, request, user_gesture, is_redirect);
        return false;
    }

    @Override
    public boolean onOpenURLFromTab(
            CefBrowser browser, CefFrame frame, String target_url, boolean user_gesture) {
        if (remoteClient != null) CefLog.Error("onOpenURLFromTab mustn't be called in remote mode (it seems that user manually called this method).");
        if (isDisposed_) return true;
        if (requestHandler_ != null && browser != null)
            return requestHandler_.onOpenURLFromTab(browser, frame, target_url, user_gesture);
        return false;
    }

    @Override
    public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame,
                                                               CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator,
                                                               BoolRef disableDefaultHandling) {
        if (remoteClient != null) CefLog.Error("getResourceRequestHandler mustn't be called in remote mode (it seems that user manually called this method).");
        if (requestHandler_ != null && browser != null) {
            return requestHandler_.getResourceRequestHandler(browser, frame, request, isNavigation,
                    isDownload, requestInitiator, disableDefaultHandling);
        }
        return null;
    }

    @Override
    public boolean getAuthCredentials(CefBrowser browser, String origin_url, boolean isProxy,
                                      String host, int port, String realm, String scheme, CefAuthCallback callback) {
        if (remoteClient != null) CefLog.Error("getAuthCredentials mustn't be called in remote mode (it seems that user manually called this method).");
        if (requestHandler_ != null && browser != null)
            return requestHandler_.getAuthCredentials(
                    browser, origin_url, isProxy, host, port, realm, scheme, callback);
        return false;
    }

    @Override
    public boolean onCertificateError(
            CefBrowser browser, ErrorCode cert_error, String request_url, CefSSLInfo sslInfo, CefCallback callback) {
        if (remoteClient != null) {
            CefLog.Error("onCertificateError mustn't be called in remote mode (it seems that user manually called this method).");
            // NOTE: next invocation us added just for IDEA tests.
            CefRequestHandler handler = remoteClient.getRequestHandler();
            if (handler != null)
                return handler.onCertificateError(browser, cert_error, request_url, sslInfo, callback);
        }
        if (requestHandler_ != null)
            return requestHandler_.onCertificateError(browser, cert_error, request_url, sslInfo, callback);
        return false;
    }

    @Override
    public void onRenderProcessTerminated(CefBrowser browser, TerminationStatus status) {
        if (remoteClient != null) CefLog.Error("onRenderProcessTerminated mustn't be called in remote mode (it seems that user manually called this method).");
        if (requestHandler_ != null) requestHandler_.onRenderProcessTerminated(browser, status);
    }

    // CefWindowHandler

    @Override
    public Rectangle getRect(CefBrowser browser) {
        if (remoteClient != null) CefLog.Error("getRect mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return new Rectangle(0, 0, 0, 0);

        CefWindowHandler realHandler = browser.getWindowHandler();
        if (realHandler != null) return realHandler.getRect(browser);
        return new Rectangle(0, 0, 0, 0);
    }

    @Override
    public void onMouseEvent(
            CefBrowser browser, int event, int screenX, int screenY, int modifier, int button) {
        if (remoteClient != null) CefLog.Error("onMouseEvent mustn't be called in remote mode (it seems that user manually called this method).");
        if (browser == null) return;

        CefWindowHandler realHandler = browser.getWindowHandler();
        if (realHandler != null)
            realHandler.onMouseEvent(browser, event, screenX, screenY, modifier, button);
    }

    public String getInfo() {
        StringBuilder sb = new StringBuilder(toString());
        if (browser_.isEmpty())
            sb.append(" [0 active browsers]");
        else {
            sb.append(" | ");
            for (CefBrowser b : browser_.values()) {
                sb.append(b);
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public static boolean isNativeBrowserCreationStarted(CefBrowser browser) {
        if (browser instanceof CefNativeAdapter)
            return ((CefNativeAdapter)browser).getNativeRef("CefBrowser") != 0;
        if (browser instanceof RemoteBrowser)
            return ((RemoteBrowser)browser).isNativeBrowserCreationStarted();
        return false;
    }
}
