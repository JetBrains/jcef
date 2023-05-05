// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefNativeAdapter;
import org.cef.handler.CefLoadHandler.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class used to represent a web response. The methods of this class may be
 * called on any thread.
 */
public abstract class CefResponseBase extends CefNativeAdapter implements CefResponse {
    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Create a new CefRequest object.
     */
    public static final CefResponse create() {
        return CefResponse_N.createNative();
    }

    /**
     * Removes the native reference from an unused object.
     */
    public abstract void dispose();

    @Override
    public String toString() { return toString(this); }

    static public String toString(CefResponse response) {
        String returnValue = "\nHTTP-Response:";

        returnValue += "\n  error: " + response.getError();
        returnValue += "\n  readOnly: " + response.isReadOnly();
        returnValue += "\n    HTTP/1.1 " + response.getStatus() + " " + response.getStatusText();
        returnValue += "\n    Content-Type: " + response.getMimeType();

        Map<String, String> headerMap = new HashMap<>();
        response.getHeaderMap(headerMap);
        Set<Entry<String, String>> entrySet = headerMap.entrySet();
        for (Entry<String, String> entry : entrySet) {
            returnValue += "    " + entry.getKey() + "=" + entry.getValue() + "\n";
        }

        return returnValue;
    }
}
