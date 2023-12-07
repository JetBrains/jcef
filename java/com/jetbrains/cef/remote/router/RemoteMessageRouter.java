package com.jetbrains.cef.remote.router;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefMessageRouterHandler;

// Simple wrapper for convenience
public class RemoteMessageRouter extends CefMessageRouter {
    private final RemoteMessageRouterImpl myImpl;

    public RemoteMessageRouter(RemoteMessageRouterImpl impl) {
        super();
        myImpl = impl;
    }

    public RemoteMessageRouterImpl getImpl() {
        return myImpl;
    }

    @Override
    public void dispose() {
        myImpl.dispose();
    }

    @Override
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        return myImpl.addHandler(handler, first);
    }

    @Override
    public boolean removeHandler(CefMessageRouterHandler handler) {
        return myImpl.removeHandler(handler);
    }

    @Override
    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {
        myImpl.cancelPending(browser, handler);
    }
}
