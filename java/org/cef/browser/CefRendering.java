package org.cef.browser;

import org.cef.handler.CefRenderHandler;

/**
 * Enum-like class: how to render CEF
 */
public abstract class CefRendering {
    /**
     * Render by Chrome engine
     */
    public static final CefRendering DEFAULT = new CefRendering() {
    };
    /**
     * Render in offscreen mode, but using JCEF component
     */
    public static final CefRendering OFFSCREEN = new CefRendering() {
    };

    private CefRendering() {
    }

    /**
     * Render in offscreen mode and forward all commands to {@link CefRenderHandler}
     */
    public static final class CefRenderingWithHandler extends CefRendering {
        private final CefRenderHandler renderHandler_;

        public CefRenderingWithHandler(CefRenderHandler renderHandler) {
            this.renderHandler_ = renderHandler;
        }

        CefRenderHandler getRenderHandler() {
            return renderHandler_;
        }
    }
}
