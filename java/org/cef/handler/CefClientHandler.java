// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefNative;
import org.cef.callback.CefNativeAdaperMulti;

import java.util.HashMap;
import java.util.Vector;

/**
 * Implement this interface to provide handler implementations.
 */
public abstract class CefClientHandler extends CefClientHandlerBase {
    private Vector<CefMessageRouter> msgRouters = new Vector<>();

    public CefClientHandler() {
        try {
            N_CefClientHandler_CTOR();
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void dispose() {
        try {
            // Call native DTOR if handler will be destroyed
            for (int i = 0; i < msgRouters.size(); i++) {
                msgRouters.get(i).dispose();
            }
            msgRouters.clear();

            N_CefClientHandler_DTOR();
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected synchronized void addMessageRouter(CefMessageRouter h) {
        try {
            msgRouters.add(h);
            N_addMessageRouter(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeContextMenuHandler(CefContextMenuHandler h) {
        try {
            N_removeContextMenuHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeDialogHandler(CefDialogHandler h) {
        try {
            N_removeDialogHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeDisplayHandler(CefDisplayHandler h) {
        try {
            N_removeDisplayHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeDownloadHandler(CefDisplayHandler h) {
        try {
            N_removeDownloadHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeDragHandler(CefDragHandler h) {
        try {
            N_removeDragHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeFocusHandler(CefFocusHandler h) {
        try {
            N_removeFocusHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeJSDialogHandler(CefJSDialogHandler h) {
        try {
            N_removeJSDialogHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeKeyboardHandler(CefKeyboardHandler h) {
        try {
            N_removeKeyboardHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeLifeSpanHandler(CefLifeSpanHandler h) {
        try {
            N_removeLifeSpanHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeLoadHandler(CefLoadHandler h) {
        try {
            N_removeLoadHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removePrintHandler(CefPrintHandler h) {
        try {
            N_removePrintHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected synchronized void removeMessageRouter(CefMessageRouter h) {
        try {
            msgRouters.remove(h);
            N_removeMessageRouter(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeRenderHandler(CefRenderHandler h) {
        try {
            N_removeRenderHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeRequestHandler(CefRequestHandler h) {
        try {
            N_removeRequestHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    protected void removeWindowHandler(CefWindowHandler h) {
        try {
            N_removeWindowHandler(h);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
    }

    private final native void N_CefClientHandler_CTOR();
    private final native void N_addMessageRouter(CefMessageRouter h);
    private final native void N_removeContextMenuHandler(CefContextMenuHandler h);
    private final native void N_removeDialogHandler(CefDialogHandler h);
    private final native void N_removeDisplayHandler(CefDisplayHandler h);
    private final native void N_removeDownloadHandler(CefDisplayHandler h);
    private final native void N_removeDragHandler(CefDragHandler h);
    private final native void N_removeFocusHandler(CefFocusHandler h);
    private final native void N_removeJSDialogHandler(CefJSDialogHandler h);
    private final native void N_removeKeyboardHandler(CefKeyboardHandler h);
    private final native void N_removeLifeSpanHandler(CefLifeSpanHandler h);
    private final native void N_removeLoadHandler(CefLoadHandler h);
    private final native void N_removePrintHandler(CefPrintHandler h);
    private final native void N_removeMessageRouter(CefMessageRouter h);
    private final native void N_removeRenderHandler(CefRenderHandler h);
    private final native void N_removeRequestHandler(CefRequestHandler h);
    private final native void N_removeWindowHandler(CefWindowHandler h);
    private final native void N_CefClientHandler_DTOR();
}
