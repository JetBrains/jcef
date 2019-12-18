package org.cef.jdk;

import org.cef.OS;
import sun.awt.AWTAccessor;

import javax.swing.*;
import java.awt.*;
import java.awt.peer.WindowPeer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Provides access to internal JDK API.
 */
public class JdkEx {
    public static class InvokeOnToolkitHelperAccessor {
        private static final Method mInvoke;

        static {
            Method m = null;
            try {
                Class<?> c = Class.forName("sun.awt.InvokeOnToolkitHelper");
                m = c.getDeclaredMethod("invokeAndBlock", Callable.class);
                m.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchMethodException ignore) {
            }
            mInvoke = m;
        }

        public static <T> T invokeAndBlock(Callable<T> callback, T defValue) {
            if (!isEnabled()) {
                try {
                    return callback.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                //noinspection unchecked
                return (T) mInvoke.invoke(null, callback);
            } catch (IllegalAccessException | InvocationTargetException ignore) {
                return defValue;
            }
        }

        public static boolean isEnabled() {
            // currently the accessor is actual on macOS only
            return OS.isMacintosh() && mInvoke != null;
        }
    }

    public static class WindowHandleAccessor {
        private static final Method mGetWindowHandle;

        static {
            Method m;
            try {
                //noinspection JavaReflectionMemberAccess
                m = WindowPeer.class.getDeclaredMethod("getWindowHandle");
                m.setAccessible(true);
            } catch (NoSuchMethodException ignore) {
                throw new RuntimeException("jcef: failed to retrieve platform window handle");
            }
            mGetWindowHandle = m;
        }

        public static long getWindowHandle(Component comp) {
            if (comp == null) return 0;

            if (!(comp instanceof Window)) {
                comp = SwingUtilities.getWindowAncestor(comp);
            }
            WindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(comp);
            try {
                return (long) mGetWindowHandle.invoke(peer);
            } catch (IllegalAccessException | InvocationTargetException ignore) {
            }
            return 0;
        }

        public static boolean isEnabled() {
            // currently the accessor is actual on macOS only
            return OS.isMacintosh() && mGetWindowHandle != null;
        }
    }
}
