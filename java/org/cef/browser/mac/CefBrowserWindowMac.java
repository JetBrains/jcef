// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser.mac;

import org.cef.browser.CefBrowserWindow;

import java.awt.*;
import java.awt.peer.ComponentPeer;

import com.jetbrains.cef.JdkEx;
import sun.awt.AWTAccessor;
import sun.lwawt.LWComponentPeer;
import sun.lwawt.PlatformWindow;
import sun.lwawt.macosx.CFRetainedResource;
import sun.lwawt.macosx.CPlatformWindow;

public class CefBrowserWindowMac implements CefBrowserWindow {
    @Override
    public long getWindowHandle(Component comp) {
        final long[] result = new long[1];
        while (comp != null) {
            if (comp.isLightweight()) {
                comp = comp.getParent();
                continue;
            }
            if (JdkEx.WindowHandleAccessor.isEnabled()) {
                return JdkEx.WindowHandleAccessor.getWindowHandle(comp);
            }
            @SuppressWarnings("deprecation")
            ComponentPeer peer = AWTAccessor.getComponentAccessor().getPeer(comp);
            if (peer instanceof LWComponentPeer) {
                @SuppressWarnings("rawtypes")
                PlatformWindow pWindow = ((LWComponentPeer) peer).getPlatformWindow();
                if (pWindow instanceof CPlatformWindow) {
                    ((CPlatformWindow) pWindow).execute(new CFRetainedResource.CFNativeAction() {
                        @Override
                        public void run(long l) {
                            result[0] = l;
                        }
                    });
                    break;
                }
            }
            comp = comp.getParent();
        }
        return result[0];
    }
}
