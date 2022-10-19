// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefNative;
import org.cef.handler.CefRequestContextHandler;

class CefRequestContext_N extends CefRequestContext {
    // Used internally to store a pointer to the CEF object.
    private static CefRequestContext_N globalInstance = null;
    private CefRequestContextHandler handler = null;

    CefRequestContext_N() {
        super();
    }

    static final CefRequestContext_N getGlobalContextNative() {
        CefRequestContext_N result = null;
        try {
            result = CefRequestContext_N.N_GetGlobalContext();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }

        if (globalInstance == null) {
            globalInstance = result;
        } else if (globalInstance.getNativeRef() == result.getNativeRef()) {
            result.N_CefRequestContext_DTOR();
        }
        return globalInstance;
    }

    static final CefRequestContext_N createNative(CefRequestContextHandler handler) {
        CefRequestContext_N result = null;
        try {
            result = CefRequestContext_N.N_CreateContext(handler);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        if (result != null) result.handler = handler;
        return result;
    }

    @Override
    public void dispose() {
        try {
            N_CefRequestContext_DTOR();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean isGlobal() {
        try {
            return N_IsGlobal();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public CefRequestContextHandler getHandler() {
        return handler;
    }

    @Override
    public void ClearCertificateExceptions(CefCompletionCallback callback) {
        N_ClearCertificateExceptions(getNativeRef(), callback);
    }

    @Override
    public void CloseAllConnections(CefCompletionCallback callback) {
        N_CloseAllConnections(getNativeRef(), callback);
    }

    private final static native CefRequestContext_N N_GetGlobalContext();
    private final static native CefRequestContext_N N_CreateContext(
            CefRequestContextHandler handler);
    private final native boolean N_IsGlobal();
    private final native void N_CefRequestContext_DTOR();
    private final native void N_ClearCertificateExceptions(long self, CefCompletionCallback callback);
    private final native void N_CloseAllConnections(long self, CefCompletionCallback callback);
}
