package com.jetbrains.cef.remote.router;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RpcExecutor;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.misc.CefLog;

import java.util.ArrayList;
import java.util.List;

// Simple wrapper for convenience
public class RemoteMessageRouter extends CefMessageRouter {
    private RemoteMessageRouterImpl myImpl;
    private final List<Runnable> myDelayedActions = new ArrayList<>();
    private volatile boolean myIsDisposed = false;

    private void execute(Runnable nativeRunnable, String name) {
        synchronized (myDelayedActions) {
            if (myIsDisposed)
                return;
            if (myImpl != null)
                nativeRunnable.run();
            else {
                CefLog.Debug("RemoteMessageRouter: %s: add delayed action %s", this, name);
                myDelayedActions.add(nativeRunnable);
            }
        }
    }

    @Override
    public void dispose() {
        synchronized (myDelayedActions) {
            myIsDisposed = true;
            myDelayedActions.clear();
            if (myImpl != null)
                myImpl.disposeOnServer();
        }
    }

    @Override
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        execute(() -> myImpl.addHandler(handler, first), "addHandler");
        return true;
    }

    @Override
    public boolean removeHandler(CefMessageRouterHandler handler) {
        execute(() -> myImpl.removeHandler(handler), "removeHandler");
        return true;
    }

    @Override
    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {
        execute(() -> myImpl.cancelPending(browser, handler), "cancelPending");
    }

    public RemoteMessageRouter(CefMessageRouterConfig config) {
        super(config);
        // NOTE: message router must be registered before browser created, so use flag 'first' here
        CefServer.instance().onConnected(()->{
            RpcExecutor service = CefServer.instance().getService();
            if (!service.isValid()) // impossible, add logging just for insurance
                CefLog.Error("Trying to create RemoteMessageRouter when not connected to server.");
            myImpl = RemoteMessageRouterImpl.create(service, getMessageRouterConfig());
            synchronized (myDelayedActions) {
                myDelayedActions.forEach(r -> r.run());
                myDelayedActions.clear();
                if (myIsDisposed && myImpl != null)
                    myImpl.disposeOnServer();
            }
        }, "MessageRouter_Create", true);
    }

    public RemoteMessageRouterImpl getImpl() {
        return myImpl;
    }
}
