// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;

/**
 * Creates a new instance of CefBrowser according the passed values
 */
public class CefBrowserFactory {
    /**
     * Cef with it's own component to render either real or offscreen.
     *
     * @deprecated use {@link #create(CefClient, String, CefRendering, boolean, CefRequestContext)}
     */
    @Deprecated
    public static CefBrowser create(CefClient client, String url, boolean isOffscreenRendered,
                                    boolean isTransparent, CefRequestContext context) {
        CefRendering rendering = isOffscreenRendered ? CefRendering.OFFSCREEN : CefRendering.DEFAULT;
        return create(client, url, rendering, isTransparent, context, null);
    }

    /**
     * Returns {@link CefBrowser} based on {@link CefRendering} passed.
     *
     * @since api-1.2
     */
    public static CefBrowser create(CefClient client, String url, CefRendering rendering,
                                    boolean isTransparent, CefRequestContext context) {
        return create(client, url, rendering, isTransparent, context, null);
    }

    /**
     * Returns {@link CefBrowser} based on {@link CefRendering} passed.
     *
     * @since api-1.14
     */
    public static CefBrowser create(CefClient client, String url, CefRendering rendering,
                                    boolean isTransparent, CefRequestContext context, CefBrowserSettings settings) {
        if (rendering == CefRendering.DEFAULT) {
            return new CefBrowserWr(client, url, context, settings);
        } else if (rendering == CefRendering.OFFSCREEN) {
            return new CefBrowserOsr(client, url, isTransparent, context, settings);
        } else if (rendering instanceof CefRendering.CefRenderingWithHandler) {
            CefRendering.CefRenderingWithHandler renderingWithHandler = (CefRendering.CefRenderingWithHandler) rendering;
            return new CefBrowserOsrWithHandler(client, url, context, renderingWithHandler.getRenderHandler(), renderingWithHandler.getComponent(), settings);
        }
        throw new IllegalArgumentException(rendering.toString());
    }
}
