// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefNative;

import java.util.Map;

class CefRequest_N extends CefRequest{
    CefRequest_N() {
        super();
    }

    public static CefRequest createNative() {
        try {
            return CefRequest_N.N_Create();
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
    public long getIdentifier() {
        try {
            return N_GetIdentifier(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return 0;
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
    public String getURL() {
        try {
            return N_GetURL(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void setURL(String url) {
        try {
            N_SetURL(getNativeRef(), url);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String getMethod() {
        try {
            return N_GetMethod(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void setMethod(String string) {
        try {
            N_SetMethod(getNativeRef(), string);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void setReferrer(String url, ReferrerPolicy policy) {
        try {
            N_SetReferrer(getNativeRef(), url, policy);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String getReferrerURL() {
        try {
            return N_GetReferrerURL(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public ReferrerPolicy getReferrerPolicy() {
        try {
            return N_GetReferrerPolicy(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public CefPostData getPostData() {
        try {
            return N_GetPostData(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void setPostData(CefPostData postData) {
        try {
            N_SetPostData(getNativeRef(), postData);
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
    public void set(
            String url, String method, CefPostData postData, Map<String, String> headerMap) {
        try {
            N_Set(getNativeRef(), url, method, postData, headerMap);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public int getFlags() {
        try {
            return N_GetFlags(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return 0;
    }

    @Override
    public void setFlags(int flags) {
        try {
            N_SetFlags(getNativeRef(), flags);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String getFirstPartyForCookies() {
        try {
            return N_GetFirstPartyForCookies(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void setFirstPartyForCookies(String url) {
        try {
            N_SetFirstPartyForCookies(getNativeRef(), url);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public ResourceType getResourceType() {
        try {
            return N_GetResourceType(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return ResourceType.RT_MAIN_FRAME;
    }

    @Override
    public TransitionType getTransitionType() {
        try {
            return N_GetTransitionType(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return TransitionType.TT_AUTO_SUBFRAME;
    }

    private final native static CefRequest_N N_Create();
    private final native void N_Dispose(long self);
    private final native long N_GetIdentifier(long self);
    private final native boolean N_IsReadOnly(long self);
    private final native String N_GetURL(long self);
    private final native void N_SetURL(long self, String url);
    private final native String N_GetMethod(long self);
    private final native void N_SetMethod(long self, String method);
    private final native void N_SetReferrer(long self, String url, ReferrerPolicy policy);
    private final native String N_GetReferrerURL(long self);
    private final native ReferrerPolicy N_GetReferrerPolicy(long self);
    private final native CefPostData N_GetPostData(long self);
    private final native void N_SetPostData(long self, CefPostData postData);
    private final native String N_GetHeaderByName(long self, String name);
    private final native void N_SetHeaderByName(
            long self, String name, String value, boolean overwrite);
    private final native void N_GetHeaderMap(long self, Map<String, String> headerMap);
    private final native void N_SetHeaderMap(long self, Map<String, String> headerMap);
    private final native void N_Set(long self, String url, String method, CefPostData postData,
            Map<String, String> headerMap);
    private final native int N_GetFlags(long self);
    private final native void N_SetFlags(long self, int flags);
    private final native String N_GetFirstPartyForCookies(long self);
    private final native void N_SetFirstPartyForCookies(long self, String url);
    private final native ResourceType N_GetResourceType(long self);
    private final native TransitionType N_GetTransitionType(long self);
}
