// Copyright (c) 2017 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.callback.CefNativeAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents all methods which are connected to the
 * native counterpart CEF.
 * The visibility of this class is "package".
 */
class CefFrame_N extends CefNativeAdapter implements CefFrame {
    // Added temporarily to avoid API changes.
    // TODO: remove later.
    private static final Map<Long, String> ourFake2Real = new HashMap<>();
    private static final Map<String, Long> ourReal2Fake = new HashMap<>();
    private static long ourFrameIdCounter = 0;

    private Long myFakeId = null;

    CefFrame_N() {}

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    @Override
    public void dispose() {
        try {
            N_Dispose(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    protected String getRealIdentifier() {
        try {
            return N_GetIdentifier(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    protected static long getFakeId(String realId) {
        synchronized (ourFake2Real) {
            Long fakeId = ourReal2Fake.get(realId);
            if (fakeId == null) {
                fakeId = ourFrameIdCounter++;
                ourFake2Real.put(fakeId, realId);
                ourReal2Fake.put(realId, fakeId);
            }
            return fakeId;
        }
    }

    protected static String getRealId(long fakeId) {
        return ourFake2Real.get(fakeId);
    }

    @Override
    public long getIdentifier() {
        if (myFakeId == null)
            myFakeId = getFakeId(getRealIdentifier());
        return myFakeId;
    }

    @Override
    public String getURL() {
        try {
            return N_GetURL(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public String getName() {
        try {
            return N_GetName(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isMain() {
        try {
            return N_IsMain(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isValid() {
        try {
            return N_IsValid(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isFocused() {
        try {
            return N_IsFocused(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return false;
        }
    }

    @Override
    public CefFrame getParent() {
        try {
            return N_GetParent(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        try {
            N_ExecuteJavaScript(getNativeRef(null), code, url, line);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void undo() {
        try {
            N_Undo(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void redo() {
        try {
            N_Redo(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void cut() {
        try {
            N_Cut(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void copy() {
        try {
            N_Copy(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void paste() {
        try {
            N_Paste(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void delete() {
        try {
            N_Delete(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void selectAll() {
        try {
            N_SelectAll(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private final native void N_Dispose(long self);
    private final native String N_GetIdentifier(long self);
    private final native String N_GetURL(long self);
    private final native String N_GetName(long self);
    private final native boolean N_IsMain(long self);
    private final native boolean N_IsValid(long self);
    private final native boolean N_IsFocused(long self);
    private final native CefFrame N_GetParent(long self);
    private final native void N_ExecuteJavaScript(long self, String code, String url, int line);
    private final native void N_Undo(long self);
    private final native void N_Redo(long self);
    private final native void N_Cut(long self);
    private final native void N_Copy(long self);
    private final native void N_Paste(long self);
    private final native void N_Delete(long self);
    private final native void N_SelectAll(long self);
}
