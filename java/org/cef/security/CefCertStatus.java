// Copyright (c) 2022 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.security;

public enum CefCertStatus {
    CERT_STATUS_NONE(0),
    CERT_STATUS_COMMON_NAME_INVALID(1),
    CERT_STATUS_DATE_INVALID(1 << 1),
    CERT_STATUS_AUTHORITY_INVALID(1 << 2),
    // 1 << 3 is reserved for ERR_CERT_CONTAINS_ERRORS (not useful with WinHTTP).
    CERT_STATUS_NO_REVOCATION_MECHANISM(1 << 4),
    CERT_STATUS_UNABLE_TO_CHECK_REVOCATION(1 << 5),
    CERT_STATUS_REVOKED(1 << 6),
    CERT_STATUS_INVALID(1 << 7),
    CERT_STATUS_WEAK_SIGNATURE_ALGORITHM(1 << 8),
    // 1 << 9 was used for CERT_STATUS_NOT_IN_DNS
    CERT_STATUS_NON_UNIQUE_NAME(1 << 10),
    CERT_STATUS_WEAK_KEY(1 << 11),
    // 1 << 12 was used for CERT_STATUS_WEAK_DH_KEY
    CERT_STATUS_PINNED_KEY_MISSING(1 << 13),
    CERT_STATUS_NAME_CONSTRAINT_VIOLATION(1 << 14),
    CERT_STATUS_VALIDITY_TOO_LONG(1 << 15),
    // Bits 16 to 31 are for non-error statuses.
    CERT_STATUS_IS_EV(1 << 16),
    CERT_STATUS_REV_CHECKING_ENABLED(1 << 17),
    // Bit 18 was CERT_STATUS_IS_DNSSEC
    CERT_STATUS_SHA1_SIGNATURE_PRESENT(1 << 19),
    CERT_STATUS_CT_COMPLIANCE_FAILED(1 << 20);

    private final int statusBitmask;

    CefCertStatus(int statusBitmask) {
        this.statusBitmask = statusBitmask;
    }

    public boolean hasStatus(int bitset) {
        return (bitset & statusBitmask) == statusBitmask;
    }
}
