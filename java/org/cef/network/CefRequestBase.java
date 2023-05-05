// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefNativeAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class CefRequestBase extends CefNativeAdapter implements CefRequest {
    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Create a new CefRequest object.
     */
    public static final CefRequest create() {
        return CefRequest_N.createNative();
    }

    /**
     * Removes the native reference from an unused object.
     */
    public abstract void dispose();

    @Override
    public String toString() { return toString(this); }

    static public String toString(CefRequest request) {
        String returnValue = "\nHTTP-Request";
        returnValue += "\n  flags: " + request.getFlags();
        returnValue += "\n  resourceType: " + request.getResourceType();
        returnValue += "\n  transitionType: " + request.getTransitionType();
        returnValue += "\n  firstPartyForCookies: " + request.getFirstPartyForCookies();
        returnValue += "\n  referrerURL: " + request.getReferrerURL();
        returnValue += "\n  referrerPolicy: " + request.getReferrerPolicy();
        returnValue += "\n    " + request.getMethod() + " " + request.getURL() + " HTTP/1.1\n";

        Map<String, String> headerMap = new HashMap<>();
        request.getHeaderMap(headerMap);
        Set<Entry<String, String>> entrySet = headerMap.entrySet();
        String mimeType = null;
        for (Entry<String, String> entry : entrySet) {
            String key = entry.getKey();
            returnValue += "    " + key + "=" + entry.getValue() + "\n";
            if (key.equals("Content-Type")) {
                mimeType = entry.getValue();
            }
        }

        CefPostData pd = request.getPostData();
        if (pd != null) {
            returnValue += CefPostDataBase.toString(mimeType, pd);
        }

        return returnValue;
    }
}
