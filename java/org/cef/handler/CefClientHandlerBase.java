package org.cef.handler;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefNativeAdaperMulti;

public abstract class CefClientHandlerBase extends CefNativeAdaperMulti {
    /**
     * Returns the java part of the browser implementation.
     * @param identifer the unique identifier of the browser.
     * @return The found browser or null if none is found.
     */
    abstract protected CefBrowser getBrowser(int identifier);

    /**
     * Returns a list of all browser instances.
     * @return an array of browser Instances.
     */
    abstract protected Object[] getAllBrowser();

    /**
     * Return the handler for context menus. If no handler is provided the
     * default implementation will be used.
     */
    abstract protected CefContextMenuHandler getContextMenuHandler();

    /**
     * Return the handler for dialogs. If no handler is provided the
     * default implementation will be used.
     */
    abstract protected CefDialogHandler getDialogHandler();

    /**
     * Return the handler for browser display state events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefDisplayHandler getDisplayHandler();

    /**
     * Return the handler for download events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefDownloadHandler getDownloadHandler();

    /**
     * Return the handler for drag events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefDragHandler getDragHandler();

    /**
     * Return the handler for focus events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefFocusHandler getFocusHandler();

    /**
     * Return the handler for media access requests.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefPermissionHandler getPermissionHandler();

    /**
     * Return the handler for javascript dialog requests.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefJSDialogHandler getJSDialogHandler();

    /**
     * Return the handler for keyboard events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefKeyboardHandler getKeyboardHandler();

    /**
     * Return the handler for browser life span events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefLifeSpanHandler getLifeSpanHandler();

    /**
     * Return the handler for browser load status events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefLoadHandler getLoadHandler();

    /**
     * Return the handler for printing on Linux. If a print handler is not
     * provided then printing will not be supported on the Linux platform.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefPrintHandler getPrintHandler();

    /**
     * Return the handler for off-screen rendering events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefRenderHandler getRenderHandler();

    /**
     * Return the handler for browser request events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefRequestHandler getRequestHandler();

    /**
     * Return the handler for windowed rendering events.
     * This method is a callback method and is called by
     * the native code.
     */
    abstract protected CefWindowHandler getWindowHandler();
}
