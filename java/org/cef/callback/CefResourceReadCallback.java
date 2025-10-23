// Copyright (c) 2024 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.callback;

/**
 * Callback interface used for asynchronous resource reading.
 */
public interface CefResourceReadCallback {
    /**
     * Callback for asynchronous continuation of Read(). If |bytes_read| == 0
     * the response will be considered complete. If |bytes_read| > 0 then Read()
     * will be called again until the request is complete (based on either the
     * result or the expected content length). If |bytes_read| < 0 then the
     * request will fail and the |bytes_read| value will be treated as the error
     * code.
     */
    void Continue(int bytes_read);

    /**
     * Returns the byte buffer to write data into before calling #Continue(int).
     */
    public byte[] getBuffer();
}
