package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @requires (os.arch == "amd64" | os.arch == "x86_64" | (os.arch == "aarch64" & os.family == "mac"))
 * @summary Tests that JCEF starts and loads empty page with no crash
 * @author Anton Tarasov
 */
@ExtendWith(TestSetupExtension.class)
public class JCEFStartupTest {
    static final CountDownLatch LATCH = new CountDownLatch(1);
    static volatile boolean PASSED;

    static volatile JBCefBrowser ourBrowser;

    private final JFrame myFrame;

    JCEFStartupTest() {
        myFrame = new JFrame("JCEF");

        ourBrowser = new JBCefBrowser();
        ourBrowser.getCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest.TransitionType transitionType) {
                System.out.println("onLoadStart");
            }
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame cefFrame, int i) {
                System.out.println("onLoadEnd");
                PASSED = true;
                LATCH.countDown();
            }
            @Override
            public void onLoadError(CefBrowser cefBrowser, CefFrame cefFrame, ErrorCode errorCode, String s, String s1) {
                System.out.println("onLoadError");
            }
        });

        myFrame.add(ourBrowser.getComponent());

        myFrame.setSize(640, 480);
        myFrame.setLocationRelativeTo(null);
        myFrame.setVisible(true);
    }

    @Test
    public void test() {
        _test(true);
    }
    @Test
    public void testJBR2222() {
        _test(false);
    }
    private void _test(boolean disposeBrowser) {
        EventQueue.invokeLater(JCEFStartupTest::new);

        try {
            LATCH.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (disposeBrowser) {
            ourBrowser.dispose();
        }
        myFrame.dispose();

        if (!PASSED) {
            throw new RuntimeException("Test FAILED!");
        }
        System.out.println("Test PASSED");
    }
}
