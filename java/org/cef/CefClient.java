// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef;

import org.cef.browser.CefMessageRouter;
import org.cef.handler.*;

/**
 * Client that owns a browser and renderer.
 */
public interface CefClient extends Disposable {
    // CefContextMenuHandler
    CefClient addContextMenuHandler(CefContextMenuHandler handler);
    void removeContextMenuHandler();

    // CefDialogHandler
    CefClient addDialogHandler(CefDialogHandler handler);
    void removeDialogHandler();

    // CefDisplayHandler
    CefClient addDisplayHandler(CefDisplayHandler handler);
    void removeDisplayHandler();

    // CefDownloadHandler
    CefClient addDownloadHandler(CefDownloadHandler handler);
    void removeDownloadHandler();

    // CefDragHandler
    CefClient addDragHandler(CefDragHandler handler);
    void removeDragHandler();

    // CefFocusHandler
    CefClient addFocusHandler(CefFocusHandler handler);
    void removeFocusHandler();

    // CefPermissionHandler
    CefClient addPermissionHandler(CefPermissionHandler handler);
    void removePermissionHandler();

    // CefJSDialogHandler
    CefClient addJSDialogHandler(CefJSDialogHandler handler);
    void removeJSDialogHandler();

    // CefKeyboardHandler
    CefClient addKeyboardHandler(CefKeyboardHandler handler);
    void removeKeyboardHandler();

    // CefLifeSpanHandler
    CefClient addLifeSpanHandler(CefLifeSpanHandler handler);
    void removeLifeSpanHandler();

    // CefLoadHandler
    CefClient addLoadHandler(CefLoadHandler handler);
    void removeLoadHandler();

    // CefPrintHandler
    CefClient addPrintHandler(CefPrintHandler handler);
    void removePrintHandler();

    // CefMessageRouter
    void addMessageRouter(CefMessageRouter messageRouter);
    void removeMessageRouter(CefMessageRouter messageRouter);

    // CefRequestHandler
    CefClient addRequestHandler(CefRequestHandler handler);
    void removeRequestHandler();
}
