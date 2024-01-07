package org.cef.browser;

import org.cef.handler.CefRenderHandler;

import java.awt.*;

/**
 * Enum-like class: how to render CEF
 *
 * @since api-1.2
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
        private final Component osrComponent_;

        public CefRenderingWithHandler(CefRenderHandler renderHandler, Component osrComponent) {
            this.renderHandler_ = renderHandler;
            osrComponent_ = osrComponent;
        }

        public CefRenderHandler getRenderHandler() {
            return renderHandler_;
        }

        public Component getComponent() {
            return osrComponent_;
        }
    }
}
