// Copyright (c) 2022 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


/**
 * The class represents a {@link X509Certificate} chain including the subject certificate.
 */
public final class CefX509Certificate {
    private X509Certificate[] chain_;

    public CefX509Certificate(byte[][] chainDERData) {
        chain_ = new X509Certificate[chainDERData.length];
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (int i = 0; i < chainDERData.length; ++i) {
                InputStream in = new ByteArrayInputStream(chainDERData[i]);
                chain_[i] = (X509Certificate) factory.generateCertificate(in);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.chain_ = null;
        }
    }

    /**
     * @return The subject certificate
     */
    public X509Certificate getSubjectCertificate() {
        if (chain_ == null || chain_.length == 0) {
            return null;
        }
        return chain_[0];
    }

    /**
     * @return The certificates chain including the subject certificate. Ordered from subject to the issuers.
     */
    public X509Certificate[] getCertificatesChain() {
        return chain_;
    }
}
