// Copyright (c) 2022 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.security;

/**
 * The class aggregates {@link CefX509Certificate} with its status bitset(see {@link org.cef.security.CefCertStatus}).
 */

public class CefSSLInfo {
    public CefSSLInfo(int statusBitset, CefX509Certificate certificate) {
        this.statusBiset = statusBitset;
        this.certificate = certificate;
    }

    public final int statusBiset;
    public final CefX509Certificate certificate;
}
