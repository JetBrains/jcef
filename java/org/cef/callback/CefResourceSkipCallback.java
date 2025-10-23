// Copyright (c) 2024 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.callback;

/**
 * Callback interface used for asynchronous resource skipping.
 */
public interface CefResourceSkipCallback {
    /**
     * Callback for asynchronous continuation of Skip(). If |bytes_skipped| > 0
     * then either Skip() will be called again until the requested number of
     * bytes have been skipped or the request will proceed. If |bytes_skipped|
     * <= 0 the request will fail with ERR_REQUEST_RANGE_NOT_SATISFIABLE.
     */
    void Continue(long bytes_skipped);
}
