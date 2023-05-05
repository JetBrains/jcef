// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.remote;

import com.jetbrains.cef.remote.CefRemoteBrowser;
import com.jetbrains.cef.remote.CefRemoteClient;
import com.jetbrains.cef.remote.CefServer;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;
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

        JBCefOsrComponent osrComponent = new JBCefOsrComponent();
        JBCefOsrHandler osrHandler = new JBCefOsrHandler(osrComponent, null);
        CefRemoteClient client = new CefRemoteClient();
        client.setRenderHandler(osrHandler);
        client.setLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                CefLog.Info("onAfterCreated " + browser);
            }
            @Override
            public boolean doClose(CefBrowser browser) {
                CefLog.Info("doClose " + browser);
                return false;
            }
            @Override
            public void onBeforeClose(CefBrowser browser) {
                CefLog.Info("onBeforeClose " + browser);
            }
        });
        client.setLoadHandler(new CefLoadHandler() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                CefLog.Info("onLoadingStateChange " + browser + " " + isLoading + ", " + canGoBack + ", " + canGoForward);
            }

            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                CefLog.Info("onLoadStart " + browser + ", " + transitionType);
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                CefLog.Info("onLoadEnd " + browser + ", " + httpStatusCode);
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                CefLog.Info("onLoadError " + browser + ", " + errorCode + ", " + errorText);
            }
        });
        CefRemoteBrowser browser = ourServer.createBrowser(client);
        if (browser == null) {
            CefLog.Error("can't create remote browser");
            return;
        }

        osrComponent.setBrowser(browser);

        JFrame frame = new JFrame("Test out of process CEF");
        JTextField address_ = new JTextField("www.google.com", 100);
        address_.addActionListener(event -> browser.loadURL(address_.getText()));
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
                ourServer.stop();
            }
        });
    }
}
