package org.cef.browser;

import org.cef.handler.CefNativeRenderHandler;
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
        @Override
        public String toString() { return "Windowed_rendering"; }
    };
    /**
     * Render in offscreen mode, but using JCEF component
     */
    public static final CefRendering OFFSCREEN = new CefRendering() {
        @Override
        public String toString() { return "Offscreen_rendering"; }
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

        @Override
        public String toString() {
            return renderHandler_ instanceof CefNativeRenderHandler ? "Offscreen_rendering_with_native_handler" : "Offscreen_rendering_with_handler";
        }
    }
}
