// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefNative;
import org.cef.handler.CefRequestContextHandler;
import org.cef.misc.CefLog;
import org.cef.misc.StringRef;
import org.cef.network.CefCookieManager;

import java.util.HashMap;
import java.util.Map;

class CefRequestContext_N extends CefRequestContext {
    // Used internally to store a pointer to the CEF object.
    private static volatile CefRequestContext_N globalInstance = null;
    private CefRequestContextHandler handler = null;

    CefRequestContext_N() {
        super();
    }

    static final synchronized CefRequestContext_N getGlobalContextNative() {
        CefRequestContext_N result = null;
        try {
            result = CefRequestContext_N.N_GetGlobalContext();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }

        if (result == null)
            return globalInstance;

        if (globalInstance == null) {
            globalInstance = result;
        } else {
            result.N_CefRequestContext_DTOR();
        }
        return globalInstance;
    }

    static final CefRequestContext_N createNative(
            CefRequestContextSettings settings, CefRequestContextHandler handler) {
        CefRequestContext_N result = null;
        try {
            result = CefRequestContext_N.N_CreateContext(settings, handler);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        if (result != null) result.handler = handler;
        return result;
    }

    static final CefRequestContext_N createNative(
            CefRequestContext other, CefRequestContextHandler handler) {
        if (!(other instanceof CefRequestContext_N)) {
            CefLog.Error("CefRequestContext_N: can't share storage with context of class %s",
                    other == null ? null : other.getClass().getName());
            return null;
        }
        CefRequestContext_N result = null;
        try {
            result = CefRequestContext_N.N_CreateContextShared((CefRequestContext_N) other, handler);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        if (result != null) result.handler = handler;
        return result;
    }

    @Override
    public void dispose() {
        if (this == globalInstance)
            return;
        try {
            N_CefRequestContext_DTOR();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean isSame(CefRequestContext other) {
        if (other == this)
            return true;
        if (!(other instanceof CefRequestContext_N))
            return false;
        try {
            return N_IsSame((CefRequestContext_N) other);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isSharingWith(CefRequestContext other) {
        if (!(other instanceof CefRequestContext_N))
            return false;
        try {
            return N_IsSharingWith((CefRequestContext_N) other);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
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
    public String getCachePath() {
        try {
            String result = N_GetCachePath();
            return result != null ? result : "";
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return "";
    }

    @Override
    public CefCookieManager getCookieManager() {
        try {
            return N_GetCookieManager();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean hasPreference(String name) {
        try {
            return N_HasPreference(name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public Object getPreference(String name) {
        try {
            return N_GetPreference(name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public Map<String, Object> getAllPreferences(boolean includeDefaults) {
        try {
            return N_GetAllPreferences(includeDefaults);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return new HashMap<String, Object>();
    }

    @Override
    public boolean canSetPreference(String name) {
        try {
            return N_CanSetPreference(name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public String setPreference(String name, Object value) {
        try {
            return N_SetPreference(name, value);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return ule.getMessage();
        }
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

    @Override
    public void clearHttpCache(CefCompletionCallback callback) {
        N_ClearHttpCache(getNativeRef(), callback);
    }

    @Override
    public void clearHttpAuthCredentials(CefCompletionCallback callback) {
        N_ClearHttpAuthCredentials(getNativeRef(), callback);
    }

    private final static native CefRequestContext_N N_GetGlobalContext();
    private final static native CefRequestContext_N N_CreateContext(
            CefRequestContextSettings settings, CefRequestContextHandler handler);
    private final static native CefRequestContext_N N_CreateContextShared(
            CefRequestContext_N other, CefRequestContextHandler handler);
    private final native boolean N_IsSame(CefRequestContext_N other);
    private final native boolean N_IsSharingWith(CefRequestContext_N other);
    private final native boolean N_IsGlobal();
    private final native String N_GetCachePath();
    private final native CefCookieManager N_GetCookieManager();
    private final native boolean N_HasPreference(String name);
    private final native Object N_GetPreference(String name);
    private final native Map<String, Object> N_GetAllPreferences(boolean includeDefaults);
    private final native boolean N_CanSetPreference(String name);
    private final native String N_SetPreference(String name, Object value);
    private final native void N_CefRequestContext_DTOR();
    private final native void N_ClearCertificateExceptions(long self, CefCompletionCallback callback);
    private final native void N_CloseAllConnections(long self, CefCompletionCallback callback);
    private final native void N_ClearHttpCache(long self, CefCompletionCallback callback);
    private final native void N_ClearHttpAuthCredentials(long self, CefCompletionCallback callback);
}
