// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.CefApp;
import org.cef.callback.CefNative;
import org.cef.handler.CefAppStateHandler;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.misc.CefLog;

import java.util.ArrayList;
import java.util.List;

class CefMessageRouter_N extends CefMessageRouter implements CefAppStateHandler {
    private boolean isNativeCtxInitialized_ = false;
    private final List<Runnable> delayedActions_ = new ArrayList<>();

    CefMessageRouter_N(CefMessageRouterConfig config) {
        super();
        setMessageRouterConfig(config);
        // NOTE: message router must be registered before browser created, so use flag 'first' here
        CefApp.getInstance().onInitialization(this, true);
    }

    @Override
    public void stateHasChanged(CefApp.CefAppState state) {
        if (CefApp.CefAppState.INITIALIZED == state) {
            N_Initialize(getMessageRouterConfig());
            synchronized (delayedActions_) {
                isNativeCtxInitialized_ = true;
                delayedActions_.forEach(r -> r.run());
                delayedActions_.clear();
            }
        }
    }

    private void executeNative(Runnable nativeRunnable, String name) {
        synchronized (delayedActions_) {
            if (isNativeCtxInitialized_)
                nativeRunnable.run();
            else {
                CefLog.Debug("CefMessageRouter_N: %s: add delayed action %s", this, name);
                delayedActions_.add(nativeRunnable);
            }
        }
    }

    @Override
    public void dispose() {
        try {
            synchronized (delayedActions_) {
                delayedActions_.clear();
                if (isNativeCtxInitialized_)
                    N_Dispose(getNativeRef());
            }
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        executeNative(() -> N_AddHandler(getNativeRef(), handler, first), "addHandler");
        return true;
    }

    @Override
    public boolean removeHandler(CefMessageRouterHandler handler) {
        executeNative(() -> N_RemoveHandler(getNativeRef(), handler), "removeHandler");
        return true;
    }

    @Override
    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {
        executeNative(() -> N_CancelPending(getNativeRef(), browser, handler), "cancelPending");
    }

    private final native void N_Initialize(CefMessageRouterConfig config);
    private final native void N_Dispose(long self);
    private final native boolean N_AddHandler(
            long self, CefMessageRouterHandler handler, boolean first);
    private final native boolean N_RemoveHandler(long self, CefMessageRouterHandler handler);
    private final native void N_CancelPending(
            long self, CefBrowser browser, CefMessageRouterHandler handler);
}
