// Copyright (c) 2024 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.callback;

class CefResourceReadCallback_N extends CefNativeAdapter implements CefResourceReadCallback {
    // The native buffer where to copy the data to.
    private long N_NativeBufferRef;

    // The Java buffer which the application is expected to fill with data.
    private byte[] N_JavaBuffer;

    CefResourceReadCallback_N() {}

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void setBufferRefs(long nativeBufferRef, byte[] javaBuffer) {
        N_NativeBufferRef = nativeBufferRef;
        N_JavaBuffer = javaBuffer;
    }

    @Override
    public byte[] getBuffer() {
        return N_JavaBuffer;
    }

    @Override
    public void Continue(int bytes_read) {
        try {
            if (N_NativeBufferRef != 0 && N_JavaBuffer != null) {
                N_Continue(getNativeRef(null), bytes_read, N_NativeBufferRef, N_JavaBuffer);
                N_NativeBufferRef = 0;
                N_JavaBuffer = null;
            }
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private final native void N_Continue(
            long self, int bytes_read, long nativeBufferRef, byte[] javaBuffer);
}
