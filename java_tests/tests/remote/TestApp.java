// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.remote;

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
        int bid = ourServer.createBrowser(osrHandler);
        if (bid < 0) {
            CefLog.Error("can't create remote browser, error=%d", bid);
            return;
        }

        osrComponent.setRemoteBid(ourServer, bid);

        JFrame frame = new JFrame("Test out of process CEF");
        JTextField address_ = new JTextField("www.google.com", 100);
        frame.getContentPane().add(address_, BorderLayout.NORTH);
        frame.getContentPane().add(osrComponent, BorderLayout.CENTER);
        frame.pack();
        frame.setSize(2000, 1400);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                ourServer.closeBrowser(bid);
                ourServer.stop();
            }
        });
    }
}
