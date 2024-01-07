package com.jetbrains.cef.remote.router;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefAppStateHandler;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.misc.CefLog;

import java.util.ArrayList;
import java.util.List;

// Simple wrapper for convenience
public class RemoteMessageRouter extends CefMessageRouter implements CefAppStateHandler {
    private RemoteMessageRouterImpl myImpl;
    private final List<Runnable> delayedActions_ = new ArrayList<>();

    @Override
    public void stateHasChanged(CefAppState state) {
        if (CefAppState.INITIALIZED == state) {
            myImpl = RemoteMessageRouterImpl.create(getMessageRouterConfig());
            synchronized (delayedActions_) {
                delayedActions_.forEach(r -> r.run());
                delayedActions_.clear();
            }
        }
    }

    private void execute(Runnable nativeRunnable, String name) {
        synchronized (delayedActions_) {
            if (myImpl != null)
                nativeRunnable.run();
            else {
                CefLog.Debug("RemoteMessageRouter: %s: add delayed action %s", this, name);
                delayedActions_.add(nativeRunnable);
            }
        }
    }

    @Override
    public void dispose() {
        try {
            synchronized (delayedActions_) {
                delayedActions_.clear();
                if (myImpl != null)
                    myImpl.dispose();
            }
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
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
        CefApp.getInstance().onInitialization(this, true);
    }

    public RemoteMessageRouterImpl getImpl() {
        return myImpl;
    }
}
