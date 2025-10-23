// Copyright (c) 2024 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.callback;

class CefResourceSkipCallback_N extends CefNativeAdapter implements CefResourceSkipCallback {
    CefResourceSkipCallback_N() {}

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void Continue(long bytes_skipped) {
        try {
            N_Continue(getNativeRef(null), bytes_skipped);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private final native void N_Continue(long self, long bytes_skipped);
}
