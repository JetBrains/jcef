// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.remote;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RemoteBrowser;
import com.jetbrains.cef.remote.RemoteClient;
import com.jetbrains.cef.remote.router.RemoteMessageRouter;
import org.cef.CefSettings;
import org.cef.misc.CefLog;
import tests.JBCefOsrComponent;
import tests.JBCefOsrHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;

public class TestApp extends JFrame {
    static CefServer ourServer;

    public static void main(String[] args) {
        CefLog.init(null, CefSettings.LogSeverity.LOGSEVERITY_VERBOSE);
        ourServer = new CefServer();
        List<String> cefArgs = Collections.emptyList();
        CefSettings settings = new CefSettings();
        if (!ourServer.start(cefArgs, settings)) {
            CefLog.Error("can't connect to CefServer");
            return;
        }

        RemoteClient client = new RemoteClient(ourServer);

        JBCefOsrComponent osrComponent = new JBCefOsrComponent();
        JBCefOsrHandler osrHandler = new JBCefOsrHandler(osrComponent, null);
        client.setRenderHandler(osrHandler);

        client.setLifeSpanHandler(new TestLifeSpanHandler());
        client.setLoadHandler(new TestLoadHandler());
        client.setDisplayHandler(new TestDisplayHandler());
        client.setRequestHandler(new TestRequestHandler());

        String qFunc = "testRemoteQuery";
        String qFuncCancel = "testRemoteQueryCancel";
        RemoteMessageRouter testRouter = RemoteMessageRouter.create(ourServer.getService(), qFunc, qFuncCancel);
        if (testRouter == null) {
            CefLog.Error("can't create RemoteMessageRouter");
            return;
        }

        testRouter.addHandler(new TestMessageRouterHandler(), true);
        client.addMessageRouter(testRouter);
        RemoteBrowser browser = ourServer.createBrowser(client);
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
                ourServer.stop();
            }
        });
    }
}
