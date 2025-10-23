// Copyright (c) 2024 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.misc;

/**
 * Helper class for passing long values by reference.
 */
public class LongRef {
    private long value_;

    public LongRef() {}

    public LongRef(long value) {
        value_ = value;
    }

    public void set(long value) {
        value_ = value;
    }

    public long get() {
        return value_;
    }
}
