package org.cef.browser;

import org.cef.CefClient;
import org.cef.handler.CefRenderHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

/**
 * Cef offscreen browser that forwards all events to {@link CefRenderHandler}
 *
 * @since api-1.3
 */
public final class CefBrowserOsrWithHandler extends CefBrowser_N  {
    private final CefRenderHandler renderHandler_;
    private final Component component_;

    /**
     * @see #CefBrowserOsrWithHandler(CefClient, String, CefRequestContext, CefRenderHandler, Component)
     */
    public CefBrowserOsrWithHandler(CefClient client, String url, CefRequestContext context, CefRenderHandler renderHandler) {
        this(client, url, context, renderHandler, null);
    }

    /**
     * In order for the browser to start loading call {@link #createImmediately()}.
     */
    public CefBrowserOsrWithHandler(CefClient client, String url, CefRequestContext context, CefRenderHandler renderHandler, Component component) {
        super(client, url, context, null, null);
        assert renderHandler != null : "Handler can't be null";
        this.renderHandler_ = renderHandler;
        this.component_ = component;
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return renderHandler_;
    }

    @Override
    public void createImmediately() {
        createBrowser(getClient(), 0, getUrl(), true, false, null, getRequestContext());
    }

    @Override
    public Component getUIComponent() {
        return component_;
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url, CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return null;
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        throw new UnsupportedOperationException("createScreenshot is not yet supported in this class");
    }
}
