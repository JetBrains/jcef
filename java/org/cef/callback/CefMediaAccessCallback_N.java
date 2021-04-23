// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.callback;

class CefMediaAccessCallback_N extends CefNativeAdapter implements CefMediaAccessCallback {
    CefMediaAccessCallback_N() {}

    @Override
    protected void finalize() throws Throwable {
        Continue(0);
        super.finalize();
    }

    @Override
    public void Continue(int allowed_permissions) {
        try {
            N_Continue(getNativeRef(null), allowed_permissions);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void Cancel() {
        try {
            N_Cancel(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private final native void N_Continue(long self, int allowed_permissions);
    private final native void N_Cancel(long self);
}
