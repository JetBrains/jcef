// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.remote;

import com.jetbrains.cef.remote.CefRemoteBrowser;
import com.jetbrains.cef.remote.CefServer;
import org.cef.misc.CefLog;
import tests.JBCefOsrComponent;
import tests.JBCefOsrHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TestApp extends JFrame {
    static CefServer ourServer;

    public static void main(String[] args) {
        ourServer = new CefServer();
        if (!ourServer.start()) {
            CefLog.Error("can't connect to CefServer");
            return;
        }

        JBCefOsrComponent osrComponent = new JBCefOsrComponent();
        JBCefOsrHandler osrHandler = new JBCefOsrHandler(osrComponent, null);
        CefRemoteBrowser browser = ourServer.createBrowser(osrHandler);
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
