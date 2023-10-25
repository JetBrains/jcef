// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefNative;
import org.cef.callback.CefNativeAdapter;
import org.cef.handler.CefLoadHandler.ErrorCode;
import org.cef.misc.DebugFormatter;

import java.util.Map;

class CefResponse_N extends CefNativeAdapter implements CefResponse {
    CefResponse_N() {
        super();
    }

    public static CefResponse createNative() {
        try {
            return CefResponse_N.N_Create();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Removes the native reference from an unused object.
     */
    private void dispose() {
        try {
            N_Dispose(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            return N_IsReadOnly(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public ErrorCode getError() {
        try {
            return N_GetError(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void setError(ErrorCode errorCode) {
        try {
            N_SetError(getNativeRef(), errorCode);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public int getStatus() {
        try {
            return N_GetStatus(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return 0;
    }

    @Override
    public void setStatus(int status) {
        try {
            N_SetStatus(getNativeRef(), status);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String getStatusText() {
        try {
            return N_GetStatusText(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void setStatusText(String statusText) {
        try {
            N_SetStatusText(getNativeRef(), statusText);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String getMimeType() {
        try {
            return N_GetMimeType(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void setMimeType(String mimeType) {
        try {
            N_SetMimeType(getNativeRef(), mimeType);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String getHeaderByName(String name) {
        try {
            return N_GetHeaderByName(getNativeRef(), name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void setHeaderByName(String name, String value, boolean overwrite) {
        try {
            N_SetHeaderByName(getNativeRef(), name, value, overwrite);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void getHeaderMap(Map<String, String> headerMap) {
        try {
            N_GetHeaderMap(getNativeRef(), headerMap);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void setHeaderMap(Map<String, String> headerMap) {
        try {
            N_SetHeaderMap(getNativeRef(), headerMap);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String toString() { return DebugFormatter.toString_Response(this); }

    private final native static CefResponse_N N_Create();
    private final native void N_Dispose(long self);
    private final native boolean N_IsReadOnly(long self);
    private final native ErrorCode N_GetError(long self);
    private final native void N_SetError(long self, ErrorCode errorCode);
    private final native int N_GetStatus(long self);
    private final native void N_SetStatus(long self, int status);
    private final native String N_GetStatusText(long self);
    private final native void N_SetStatusText(long self, String statusText);
    private final native String N_GetMimeType(long self);
    private final native void N_SetMimeType(long self, String mimeType);
    private final native String N_GetHeaderByName(long self, String name);
    private final native void N_SetHeaderByName(
            long self, String name, String value, boolean overwrite);
    private final native void N_GetHeaderMap(long self, Map<String, String> headerMap);
    private final native void N_SetHeaderMap(long self, Map<String, String> headerMap);
}
