// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.
package tests.junittests;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefCallback;
import org.cef.security.CefCertStatus;
import org.cef.security.CefSSLInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestSetupExtension.class)
class SelfSignedSSLTest {
    @Test
    void certificateAccepted() {
        var frame = new TestFrame() {
            public CefSSLInfo sslInfo = null;

            @Override
            protected void setupTest() {
                createBrowser("https://untrusted-root.badssl.com/");
                super.setupTest();
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                super.onLoadEnd(browser, frame, httpStatusCode);
                terminateTest();
            }

            @Override
            public boolean onCertificateError(CefBrowser browser, ErrorCode cert_error, String request_url, CefSSLInfo sslInfo, CefCallback callback) {
                this.sslInfo = sslInfo;
                callback.Continue();
                return true;
            }
        };

        frame.awaitCompletion();

        Assertions.assertNotNull(frame.sslInfo);
        Assertions.assertNotNull(frame.sslInfo.certificate);
        Assertions.assertTrue(CefCertStatus.CERT_STATUS_AUTHORITY_INVALID.hasStatus(frame.sslInfo.statusBiset));
    }

    @Test
    void certificateRejected() {
        var frame = new TestFrame() {
            boolean isOnCertificateErrorCalled = false;
            boolean isOnLoadErrorCalled = false;

            @Override
            protected void setupTest() {
                createBrowser("https://self-signed.badssl.com/");
                super.setupTest();
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                super.onLoadEnd(browser, frame, httpStatusCode);
                terminateTest();
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                isOnLoadErrorCalled = true;
                super.onLoadError(browser, frame, errorCode, errorText, failedUrl);
            }

            @Override
            public boolean onCertificateError(CefBrowser browser, ErrorCode cert_error, String request_url, CefSSLInfo sslInfo, CefCallback callback) {
                isOnCertificateErrorCalled = true;
                return false;
            }
        };

        frame.awaitCompletion();
        Assertions.assertTrue(frame.isOnCertificateErrorCalled);
        Assertions.assertTrue(frame.isOnLoadErrorCalled);
    }
}
