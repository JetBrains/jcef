package org.cef.browser;

import org.cef.CefClient;
import org.cef.handler.CefRenderHandler;

import java.awt.*;

/**
 * Cef offscreen browser that forwards all events to {@link CefRenderHandler}
 */
public final class CefBrowserOsrWithHandler extends CefBrowser_N implements CefBrowser {
    private final CefRenderHandler renderHandler_;

    public CefBrowserOsrWithHandler(CefClient client, String url, CefRequestContext context, CefRenderHandler renderHandler) {
        super(client, url, context, null, null);
        assert renderHandler != null : "Handler can't be null";
        this.renderHandler_ = renderHandler;
        createBrowser(client, 0, url, true, false,null, context);
    }


    @Override
    public CefRenderHandler getRenderHandler() {
        return renderHandler_;
    }

    @Override
    public void createImmediately() {
        // Created in ctor
    }

    @Override
    public Component getUIComponent() {
        throw new IllegalStateException("Handler based browser doesn't have component");
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url, CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        throw new IllegalStateException("Handler based browser doesn't have dev tools");

    }
}
