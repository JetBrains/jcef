// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefNativeAdapter;

import java.util.Vector;

/**
 * Class used to represent post data for a web request. The methods of this
 * class may be called on any thread.
 */
public abstract class CefPostDataBase extends CefNativeAdapter implements CefPostData {
    // This CTOR can't be called directly. Call method create() instead.
    CefPostDataBase() {}

    /**
     * Removes the native reference from an unused object.
     */
    public abstract void dispose();

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Create a new CefPostData object.
     */
    public static final CefPostDataBase create() {
        return CefPostData_N.createNative();
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String mimeType) {
        return toString(mimeType, this);
    }

    static public String toString(String mimeType, CefPostData pd) {
        Vector<CefPostDataElement> elements = new Vector<CefPostDataElement>();
        pd.getElements(elements);

        String returnValue = "";
        for (CefPostDataElement el : elements) {
            returnValue += CefPostDataElementBase.toString(mimeType, el) + "\n";
        }
        return returnValue;
    }
}
