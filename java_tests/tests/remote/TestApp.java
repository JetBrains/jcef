// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.remote;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RemoteBrowser;
import com.jetbrains.cef.remote.RemoteClient;
import com.jetbrains.cef.remote.router.RemoteMessageRouter;
import com.jetbrains.cef.remote.router.RemoteMessageRouterImpl;
import org.cef.CefSettings;
import org.cef.browser.CefMessageRouter;
import org.cef.misc.CefLog;
import tests.JBCefOsrComponent;
import tests.JBCefOsrHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TestApp extends JFrame {
    public static void main(String[] args) {
        CefLog.init(null, CefSettings.LogSeverity.LOGSEVERITY_VERBOSE);
        CefServer.initialize();
        if (CefServer.instance() == null)
            return;

        CefServer server = CefServer.instance();
        RemoteClient client = server.createClient();

        JBCefOsrComponent osrComponent = new JBCefOsrComponent();
        JBCefOsrHandler osrHandler = new JBCefOsrHandler(osrComponent, null);
        client.setRenderHandler(osrHandler);

        client.addLifeSpanHandler(new TestLifeSpanHandler());
        client.addLoadHandler(new TestLoadHandler());
        client.addDisplayHandler(new TestDisplayHandler());
        client.addRequestHandler(new TestRequestHandler());

        String qFunc = "testRemoteQuery";
        String qFuncCancel = "testRemoteQueryCancel";
        RemoteMessageRouterImpl testRouter = RemoteMessageRouterImpl.create(new CefMessageRouter.CefMessageRouterConfig(qFunc, qFuncCancel));
        if (testRouter == null) {
            CefLog.Error("can't create RemoteMessageRouter");
            return;
        }

        testRouter.addHandler(new TestMessageRouterHandler(), true);
        client.addMessageRouter(new RemoteMessageRouter(testRouter));
        RemoteBrowser browser = client.createBrowser("www.google.com",true,null, null);
        browser.createImmediately();
        if (browser == null) {
            CefLog.Error("can't create remote browser");
            return;
        }

        osrComponent.setBrowser(browser);

        JFrame frame = new JFrame("Test out of process CEF");
        JTextField address_ = new JTextField("www.google.com", 100);
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
    }
}
