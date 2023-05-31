// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.network;

import org.cef.callback.CefNativeAdapter;
import org.cef.misc.DebugFormatter;

import java.util.Vector;

class CefPostData_N extends CefNativeAdapter implements CefPostData {
    // This CTOR can't be called directly. Call method create() instead.
    CefPostData_N() {}

    /**
     * Create a new CefPostData object.
     */
    static final CefPostData createNative() {
        try {
            return CefPostData_N.N_Create();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
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
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
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
    public int getElementCount() {
        try {
            return N_GetElementCount(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return 0;
    }

    @Override
    public void getElements(Vector<CefPostDataElement> elements) {
        try {
            N_GetElements(getNativeRef(), elements);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean removeElement(CefPostDataElement element) {
        try {
            return N_RemoveElement(getNativeRef(), element);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean addElement(CefPostDataElement element) {
        try {
            return N_AddElement(getNativeRef(), element);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void removeElements() {
        try {
            N_RemoveElements(getNativeRef());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String mimeType) {
        return DebugFormatter.toString_PostData(mimeType, this);
    }

    private final native static CefPostData_N N_Create();
    private final native void N_Dispose(long self);
    private final native boolean N_IsReadOnly(long self);
    private final native int N_GetElementCount(long self);
    private final native void N_GetElements(long self, Vector<CefPostDataElement> elements);
    private final native boolean N_RemoveElement(long self, CefPostDataElement element);
    private final native boolean N_AddElement(long self, CefPostDataElement element);
    private final native void N_RemoveElements(long self);
}
