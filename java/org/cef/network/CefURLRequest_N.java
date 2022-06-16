// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefNative;
import org.cef.callback.CefURLRequestClient;
import org.cef.handler.CefLoadHandler.ErrorCode;

class CefURLRequest_N extends CefURLRequest {
    private final CefRequest request_;
    private final CefURLRequestClient client_;

    CefURLRequest_N(CefRequest request, CefURLRequestClient client) {
        super();
        request_ = request;
        client_ = client;
    }

    public static final CefURLRequest createNative(CefRequest request, CefURLRequestClient client) {
        // keep a reference to the request and client objects.
        CefURLRequest_N result = new CefURLRequest_N(request, client);
        try {
            result.N_Create(request, client);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        if (result.getNativeRef() == 0) return null;
        return result;
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
    public CefRequest getRequest() {
        return request_;
    }

    @Override
    public CefURLRequestClient getClient() {
        return client_;
    }

    @Override
    public Status getRequestStatus() {
        try {
            return N_GetRequestStatus(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public ErrorCode getRequestError() {
        try {
            return N_GetRequestError(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public CefResponse getResponse() {
        try {
            return N_GetResponse(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void cancel() {
        try {
            N_Cancel(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private final native void N_Create(CefRequest request, CefURLRequestClient client);
    private final native void N_Dispose(long self);
    private final native Status N_GetRequestStatus(long self);
    private final native ErrorCode N_GetRequestError(long self);
    private final native CefResponse N_GetResponse(long self);
    private final native void N_Cancel(long self);
}
