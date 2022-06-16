// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.callback.CefNative;
import org.cef.handler.CefMessageRouterHandler;

class CefMessageRouter_N extends CefMessageRouter {
    private CefMessageRouter_N() {
        super();
    }

    public static final CefMessageRouter createNative(CefMessageRouterConfig config) {
        try {
            return CefMessageRouter_N.N_Create(config);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public void dispose() {
        try {
            N_Dispose(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        try {
            return N_AddHandler(getNativeRef(), handler, first);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean removeHandler(CefMessageRouterHandler handler) {
        try {
            return N_RemoveHandler(getNativeRef(), handler);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return false;
        }
    }

    @Override
    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {
        try {
            N_CancelPending(getNativeRef(), browser, handler);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private final native static CefMessageRouter_N N_Create(CefMessageRouterConfig config);
    private final native void N_Dispose(long self);
    private final native boolean N_AddHandler(
            long self, CefMessageRouterHandler handler, boolean first);
    private final native boolean N_RemoveHandler(long self, CefMessageRouterHandler handler);
    private final native void N_CancelPending(
            long self, CefBrowser browser, CefMessageRouterHandler handler);
}
