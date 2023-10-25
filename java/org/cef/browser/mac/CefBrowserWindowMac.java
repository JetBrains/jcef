// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser.mac;

import org.cef.browser.CefBrowserWindow;

import java.awt.*;
import java.awt.peer.ComponentPeer;

import com.jetbrains.cef.JdkEx;
import sun.awt.AWTAccessor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

            ComponentPeer peer = AWTAccessor.getComponentAccessor().getPeer(comp);
            Class<?> lw = Class.forName("sun.lwawt.LWComponentPeer");

            if (lw.isInstance(peer)) {
                Method platformWindowMethod = lw.getMethod("getPlatformWindow");
                Object pWindow = platformWindowMethod.invoke(lw.cast(peer));
                Class<?> cPlatformWindow = Class.forName("sun.lwawt.macosx.CPlatformWindow");

                if (cPlatformWindow.isInstance(pWindow)) {
                    Class<?> nativeAction = Class.forName("sun.lwawt.macosx.CFRetainedResource$CFNativeAction");
                    Object nativeActionInstance = Proxy.newProxyInstance(
                            nativeAction.getClassLoader(),
                            new Class[]{nativeAction},
                            new WindowInvocationHandler(ptr -> result[0] = ptr)
                    );

                    Method execute = cPlatformWindow.getMethod("execute", nativeAction);
                    execute.invoke(pWindow, nativeActionInstance);
                }
            }
            comp = comp.getParent();
        }
        return result[0];
    }

    private static class WindowInvocationHandler implements InvocationHandler {

        private final WindowInvocationResult callback;

        WindowInvocationHandler(WindowInvocationResult callback) {
            this.callback = callback;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            callback.run((Long) args[0]);
            return proxy;
        }
    }

    private interface WindowInvocationResult {
        void run(long ptr);
    }
}
