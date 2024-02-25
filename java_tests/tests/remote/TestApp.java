// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.remote;

import com.jetbrains.cef.JCefAppConfig;
import com.jetbrains.cef.remote.CefServer;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefRendering;
import org.cef.handler.CefAppHandler;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.misc.CefLog;
import tests.JBCefOsrComponent;
import tests.JBCefOsrHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestApp extends JFrame {
    private static final String ourStartURL = "www.google.com";
    private static final boolean IS_REMOTE = isRemoteEnabled();

    public static void main(String[] args) throws InterruptedException {
        CefLog.init(null, CefSettings.LogSeverity.LOGSEVERITY_VERBOSE);

        JCefAppConfig config = JCefAppConfig.getInstance();
        List<String> appArgs = new ArrayList<>(Arrays.asList(args));
        appArgs.addAll(config.getAppArgsAsList());
        args = appArgs.toArray(new String[0]);

        CefAppHandler appHandler = new CefAppHandlerAdapter(args) {};
        CefApp.addAppHandler(appHandler);
        CefApp.startup(null);
        CefApp app = CefApp.getInstance(config.getCefSettings());
        CountDownLatch latch = new CountDownLatch(1);
        app.onInitialization(s -> latch.countDown());
        if (IS_REMOTE) {
            if (!CefServer.instance().onConnected(null, "", false)) {
                CefLog.Error("Not connected.");
                return;
            }
        }

        SwingUtilities.invokeLater(()->{
            createFrame(ourStartURL);
        });
    }

    public static JFrame createFrame(String url) {
        JFrame frame = new JFrame("Test out of process CEF");

        CefClient client = CefApp.getInstance().createClient();

        JBCefOsrComponent osrComponent = new JBCefOsrComponent();
        JBCefOsrHandler osrHandler = new JBCefOsrHandler(osrComponent, null);

        client.addLifeSpanHandler(new TestLifeSpanHandler());
        client.addLoadHandler(new TestLoadHandler());
        client.addDisplayHandler(new TestDisplayHandler() {
            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                frame.setTitle(title);
            }
        });
        client.addRequestHandler(new TestRequestHandler());

        String qFunc = "testRemoteQuery";
        String qFuncCancel = "testRemoteQueryCancel";
        CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig(qFunc, qFuncCancel);
        CefMessageRouter testRouter = CefMessageRouter.create(config);

        testRouter.addHandler(new TestMessageRouterHandler(), true);
        client.addMessageRouter(testRouter);
        CefBrowser browser = client.createBrowser(url, new CefRendering.CefRenderingWithHandler(osrHandler, osrComponent), true);
        browser.createImmediately();

        osrComponent.setBrowser(browser);

        JTextField address_ = new JTextField(url, 100);
        address_.addActionListener(event -> {
            browser.loadURL(address_.getText());
            browser.executeJavaScript("window." + qFunc + "({request: '" + qFunc + "'});", "", 0);
        });
        frame.getContentPane().add(address_, BorderLayout.NORTH);
        frame.getContentPane().add(osrComponent, BorderLayout.CENTER);
        frame.pack();
        frame.setSize(2000, 1400);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                browser.close(true);
                client.dispose();
                CefApp.getInstance().dispose();
            }
        });

        return frame;
    }

    protected static boolean isRemoteEnabled() {
        try {
            // Temporary use reflection to test with old jcef
            Method m = CefApp.class.getMethod("isRemoteEnabled");
            return (boolean)m.invoke(CefApp.class);
        }
        catch (NoSuchMethodException e) {
        }
        catch (InvocationTargetException e) {
        }
        catch (IllegalAccessException e) {
        }
        return false;
    }
}
