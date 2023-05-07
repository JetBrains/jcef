// Copyright (c) 2023 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.misc;

public class CefRange {
    public int from;
    public int to;

    public CefRange() {}

    @Override
    public String toString() {
        return "CefRange[from=" + from + ", to=" + to + "]";
    }

    public CefRange(int from, int to) {
        this.from = from;
        this.to = to;
    }
}
