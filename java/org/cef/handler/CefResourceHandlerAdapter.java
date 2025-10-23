// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.callback.CefCallback;
import org.cef.callback.CefResourceReadCallback;
import org.cef.callback.CefResourceSkipCallback;
import org.cef.misc.BoolRef;
import org.cef.misc.IntRef;
import org.cef.misc.LongRef;
import org.cef.misc.StringRef;
import org.cef.network.CefCookie;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

/**
 * An abstract adapter class for receiving resource requests.
 * The methods in this class are empty.
 * This class exists as convenience for creating handler objects.
 */
public abstract class CefResourceHandlerAdapter implements CefResourceHandler {
    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        return false;
    }

    @Override
    public boolean open(CefRequest request, BoolRef handleRequest, CefCallback callback) {
        // Enables backwards compatibility by default by calling processRequest.
        handleRequest.set(false);
        return false;
    }

    @Override
    public void getResponseHeaders(
            CefResponse response, IntRef responseLength, StringRef redirectUrl) {}

    @Override
    public boolean readResponse(
            byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        return false;
    }

    @Override
    public boolean read(
            byte[] dataOut, int bytesToRead, IntRef bytesRead, CefResourceReadCallback callback) {
        // Enables backwards compatibility by default by calling readResponse.
        bytesRead.set(-1);
        return false;
    }

    @Override
    public boolean skip(long bytesToSkip, LongRef bytesSkipped, CefResourceSkipCallback callback) {
        bytesSkipped.set(-2);
        return false;
    }

    @Override
    public void cancel() {}
}
