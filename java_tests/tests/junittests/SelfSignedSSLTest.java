// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.
package tests.junittests;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefCallback;
import org.cef.security.CefCertStatus;
import org.cef.security.CefSSLInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Base64;

@ExtendWith(TestSetupExtension.class)
class SelfSignedSSLTest {
    /*
    Base64 encoded Java Keystore Data.
    Generate a self-signed certificate:
    $ keytool -genkeypair -keyalg RSA -alias selfsigned -keystore testkey.jks -storepass password -validity 7200 -keysize 2048
    $ cat testkey.jks | base64
     */
    final static String JKS_BASE64 = "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";
    final static String STORAGE_PASSWORD = "password";

    static KeyStore makeKeyStore() {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("jks");
            ks.load(new ByteArrayInputStream(Base64.getDecoder().decode(SelfSignedSSLTest.JKS_BASE64)), STORAGE_PASSWORD.toCharArray());
        } catch (Exception e) {
            Assertions.fail("Failed to load the keystore");
        }
        return ks;
    }

    static HttpsServer makeHttpsServer(KeyStore keyStore) {
        try {
            // initialise the HTTPS server
            HttpsServer server = HttpsServer.create(new InetSocketAddress(0), 0);
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // setup the key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, STORAGE_PASSWORD.toCharArray());

            // setup the trust manager factory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // setup the HTTPS context and parameters
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    try {
                        SSLContext context = getSSLContext();
                        SSLEngine engine = context.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        SSLParameters sslParameters = context.getSupportedSSLParameters();
                        params.setSSLParameters(sslParameters);
                    } catch (Exception ex) {
                        System.out.println("Failed to create HTTPS port");
                        throw new RuntimeException(ex);
                    }
                }
            });
            server.createContext("/test", t -> {
                String response = "This is the response";
                t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                t.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            });
            return server;
        } catch (Exception e) {
            Assertions.fail("Failed to start HTTPS server. " + e);
        }
        return null;
    }

    @Test
    void certificateAccepted() {
        KeyStore keyStore = makeKeyStore();
        Certificate[] certificateChainExpected = null;
        try {
            certificateChainExpected = keyStore.getCertificateChain("selfsigned");
        } catch (KeyStoreException e) {
            Assertions.fail("Failed to get certificate chain from the key store");
        }

        HttpsServer server = makeHttpsServer(keyStore);
        server.start();

        var frame = new TestFrame() {
            public CefSSLInfo sslInfo = null;

            @Override
            protected void setupTest() {
                createBrowser("https:/" + server.getAddress());
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

        Assertions.assertArrayEquals(certificateChainExpected, frame.sslInfo.certificate.getCertificatesChain());
        Assertions.assertTrue(CefCertStatus.CERT_STATUS_AUTHORITY_INVALID.hasStatus(frame.sslInfo.statusBiset));
        server.stop(0);
    }

    @Test
    void certificateRejected() {
        KeyStore keyStore = makeKeyStore();
        HttpsServer server = makeHttpsServer(keyStore);
        server.start();

        var frame = new TestFrame() {
            boolean isOnCertificateErrorCalled = false;
            boolean isOnLoadErrorCalled = false;

            @Override
            protected void setupTest() {
                createBrowser("https:/" + server.getAddress());
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

        server.stop(0);
    }
}
