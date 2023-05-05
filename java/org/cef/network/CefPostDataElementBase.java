// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefNativeAdapter;

/**
 * Class used to represent a single element in the request post data. The
 * methods of this class may be called on any thread.
 */
public abstract class CefPostDataElementBase extends CefNativeAdapter implements CefPostDataElement {
    /**
     * Post data elements may represent either bytes or files.
     */
    public static enum Type {
        PDE_TYPE_EMPTY,
        PDE_TYPE_BYTES,
        PDE_TYPE_FILE,
    }

    // This CTOR can't be called directly. Call method create() instead.
    CefPostDataElementBase() {}

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Create a new CefPostDataElement object.
     */
    public static final CefPostDataElementBase create() {
        return CefPostDataElement_N.createNative();
    }

    /**
     * Removes the native reference from an unused object.
     */
    public abstract void dispose();


    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String mimeType) {
        return toString(mimeType, this);
    }

    static public String toString(String mimeType, CefPostDataElement e) {
        int bytesCnt = e.getBytesCount();
        byte[] bytes = null;
        if (bytesCnt > 0) {
            bytes = new byte[bytesCnt];
        }

        boolean asText = false;
        if (mimeType != null) {
            if (mimeType.startsWith("text/"))
                asText = true;
            else if (mimeType.startsWith("application/xml"))
                asText = true;
            else if (mimeType.startsWith("application/xhtml"))
                asText = true;
            else if (mimeType.startsWith("application/x-www-form-urlencoded"))
                asText = true;
        }

        String returnValue = "";

        if (e.getType() == Type.PDE_TYPE_BYTES) {
            int setBytes = e.getBytes(bytes.length, bytes);
            returnValue += "    Content-Length: " + bytesCnt + "\n";
            if (asText) {
                returnValue += "\n    " + new String(bytes);
            } else {
                for (int i = 0; i < setBytes; i++) {
                    if (i % 40 == 0) returnValue += "\n    ";
                    returnValue += String.format("%02X", bytes[i]) + " ";
                }
            }
            returnValue += "\n";
        } else if (e.getType() == Type.PDE_TYPE_FILE) {
            returnValue += "\n    Bytes of file: " + e.getFile() + "\n";
        }
        return returnValue;
    }
}
