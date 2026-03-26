package com.jetbrains.cef.remote.router;

import com.jetbrains.cef.remote.CefServer;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.misc.Delayed;

// Simple wrapper for convenience
public class RemoteMessageRouter extends CefMessageRouter {
    private RemoteMessageRouterImpl myImpl;
    private final Delayed myDelayed;

    @Override
    public void dispose() {
        myDelayed.dispose();
        if (myImpl != null)
            myImpl.disposeOnServer();
        myImpl = null;
    }

    @Override
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        myDelayed.runOrDelay(() -> myImpl.addHandler(handler, first), "addHandler");
        return true;
    }

    @Override
    public boolean removeHandler(CefMessageRouterHandler handler) {
        myDelayed.runOrDelay(() -> myImpl.removeHandler(handler), "removeHandler");
        return true;
    }

    @Override
    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {
        myDelayed.runOrDelay(() -> myImpl.cancelPending(browser, handler), "cancelPending");
    }

    public RemoteMessageRouter(CefServer server, CefMessageRouterConfig config) {
        super(config);
        myDelayed = new Delayed("RemoteMessageRouter");
        // NOTE: message router must be registered before browser created, so use flag 'first' here
        myDelayed.finishOnConnection(server, true, () -> myImpl = RemoteMessageRouterImpl.create(server.getRpcContext(), getMessageRouterConfig()));
    }

    public RemoteMessageRouterImpl getImpl() {
        return myImpl;
    }

    public void addToClient(int cid) {
        myDelayed.runOrDelay(()->{
            myImpl.addToClient(cid);
        }, "addToClient");
    }

    public void removeFromClient(int cid) {
        myDelayed.runOrDelay(()->{
            myImpl.removeFromClient(cid);
        }, "removeFromClient");
    }
}
