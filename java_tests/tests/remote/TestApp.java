// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.remote;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RemoteBrowser;
import com.jetbrains.cef.remote.RemoteClient;
import com.jetbrains.cef.remote.router.RemoteMessageRouter;
import com.jetbrains.cef.remote.router.RemoteMessageRouterImpl;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefCommandLine;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandler;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.misc.CefLog;
import tests.JBCefOsrComponent;
import tests.JBCefOsrHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TestApp extends JFrame {
    private static final String ourStartURL = "www.google.com";

    public static void main(String[] args) {
        CefLog.init(null, CefSettings.LogSeverity.LOGSEVERITY_VERBOSE);
        CefServer.initialize();
        CefServer server = CefServer.instance();
        if (server == null || !server.isInitialized())
            return;

        SwingUtilities.invokeLater(()->{
            createFrame(ourStartURL);
        });
    }

    public static JFrame createFrame(String url) {
        CefServer server = CefServer.instance();
        RemoteClient client = server.createClient();

        JBCefOsrComponent osrComponent = new JBCefOsrComponent();
        JBCefOsrHandler osrHandler = new JBCefOsrHandler(osrComponent, null);

        client.addLifeSpanHandler(new TestLifeSpanHandler());
        client.addLoadHandler(new TestLoadHandler());
        client.addDisplayHandler(new TestDisplayHandler());
        client.addRequestHandler(new TestRequestHandler());

        String qFunc = "testRemoteQuery";
        String qFuncCancel = "testRemoteQueryCancel";
        CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig(qFunc, qFuncCancel);
        RemoteMessageRouter testRouter = new RemoteMessageRouter(config);

        testRouter.addHandler(new TestMessageRouterHandler(), true);
        client.addMessageRouter(testRouter);
        RemoteBrowser browser = client.createBrowser(url,null, null, osrHandler, osrComponent);
        browser.createImmediately();
        if (browser == null) {
            CefLog.Error("can't create remote browser");
            return null;
        }

        osrComponent.setBrowser(browser);

        JFrame frame = new JFrame("Test out of process CEF");
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
                server.stop();
            }
        });

        return frame;
    }
}
