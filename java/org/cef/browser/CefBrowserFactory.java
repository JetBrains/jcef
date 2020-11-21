// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.CefClient;
import org.cef.handler.CefRenderHandler;

/**
 * Creates a new instance of CefBrowser according the passed values
 */
public class CefBrowserFactory {
    /**
     * Cef that doesn't have any component. All drawing methods are forwarded to {@link CefRenderHandler}.
     * {@link CefBrowserWithEventsSink} can me used to send mouse/keyboard events to the CEF
     */
    public static CefBrowserWithEventsSink createOffScreenWithHandler(
            CefClient client, String url, CefRenderHandler renderHandler, CefRequestContext context) {
        return new CefBrowserOsrWithHandler(client, url, context, renderHandler);
    }

    /**
     * Cef with it's own component to render either real or offscreen
     */
    public static CefBrowser create(CefClient client, String url, boolean isOffscreenRendered,
                                    boolean isTransparent, CefRequestContext context) {
        if (isOffscreenRendered) return new CefBrowserOsr(client, url, isTransparent, context);
        return new CefBrowserWr(client, url, context);
    }
}
