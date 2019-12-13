// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser.mac;

import java.awt.*;
import java.awt.peer.ComponentPeer;
import java.awt.peer.WindowPeer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import sun.awt.AWTAccessor;
import sun.lwawt.LWComponentPeer;
import sun.lwawt.PlatformWindow;
import sun.lwawt.macosx.CFRetainedResource;
import sun.lwawt.macosx.CPlatformWindow;

import org.cef.browser.CefBrowserWindow;

import javax.swing.*;

public class CefBrowserWindowMac implements CefBrowserWindow {
    @Override
    public long getWindowHandle(Component comp) {
        final long[] result = new long[1];
        while (comp != null) {
            if (comp.isLightweight()) {
                comp = comp.getParent();
                continue;
            }
            if (WindowHandle.isAvailable()) {
                return WindowHandle.get(comp);
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

    private static class WindowHandle {
        private static final Method methodGetWindowHandle;

        static {
            Method m;
            try {
                //noinspection JavaReflectionMemberAccess
                m = WindowPeer.class.getDeclaredMethod("getWindowHandle");
                m.setAccessible(true);
            } catch (NoSuchMethodException ignore) {
                throw new RuntimeException("jcef: failed to retrieve platform window handle");
            }
            methodGetWindowHandle = m;
        }

        static long get(Component comp) {
            if (comp == null) return 0;

            if (!(comp instanceof Window)) {
                comp = SwingUtilities.getWindowAncestor(comp);
            }
            WindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(comp);
            try {
                return (long)methodGetWindowHandle.invoke(peer);
            } catch (IllegalAccessException | InvocationTargetException ignore) {
            }
            return 0;
        }

        static boolean isAvailable() {
            return methodGetWindowHandle != null;
        }
    }
}
