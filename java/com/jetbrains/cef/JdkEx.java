package com.jetbrains.cef;

import org.cef.OS;
import org.cef.misc.CefLog;
import sun.awt.AWTAccessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InvocationEvent;
import java.awt.peer.WindowPeer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to internal JDK API, provides utility methods.
 */
public class JdkEx {
    public static final int INVOKE_ON_EDT_TIMEOUT;
    public static final boolean INVOKE_ON_EDT_TIMEOUT_LOG;

    static {
        int timeout = Integer.getInteger("com.jetbrains.cef.invokeOnEdtTimeout", 200);
        INVOKE_ON_EDT_TIMEOUT = timeout > 0 ? timeout : Integer.MAX_VALUE;
        INVOKE_ON_EDT_TIMEOUT_LOG = Boolean.getBoolean("com.jetbrains.cef.invokeOnEdtTimeout.log.enabled");
    }

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
            } catch (Throwable ignore) {
                CefLog.Warn("failed to retrieve platform window handle");
                m = null;
            }
            mGetWindowHandle = m;
        }

        public static long getWindowHandle(Component comp) {
            if (comp == null) return 0;

            if (!(comp instanceof Window)) {
                comp = SwingUtilities.getWindowAncestor(comp);
            }
            if (comp == null) return 0;

            @SuppressWarnings("RedundantCast") // the cast pleases jdk8 javac
            WindowPeer peer = (WindowPeer)AWTAccessor.getComponentAccessor().getPeer(comp);
            if (peer == null) return 0;
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

    public static boolean isJetBrainsJDK() {
        String vendor = System.getProperty("java.vendor");
        return vendor != null && vendor.toLowerCase().contains("jetbrains");
    }

    public static void invokeOnEDTAndWait(Runnable runnable, Object source) {
        invokeOnEDTAndWait(() -> {
            runnable.run();
            return null;
        }, null, source);
    }

    public static <T> T invokeOnEDTAndWait(Callable<T> callable, T defaultValue, Object source) {
        SyncCallable<T> syncCallable = new SyncCallable<>(callable, defaultValue);
        if (EventQueue.isDispatchThread()) {
            return syncCallable.call();
        }
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new InvocationEvent(source, syncCallable));
        return syncCallable.waitGet(INVOKE_ON_EDT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private static class SyncCallable<T> implements Callable<T>, Runnable {
        volatile Callable<T> callable;
        volatile T result;
        volatile T defaultValue;

        final CountDownLatch latch = new CountDownLatch(1);

        public SyncCallable(Callable<T> callable, T defaultValue) {
            this.callable = callable;
            this.defaultValue = defaultValue;
        }

        @Override
        public void run() {
            try {
                result = callable.call();
            } catch (Exception e) {
                result = defaultValue;
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }

        @Override
        public T call() {
            run();
            return get();
        }

        public T get() {
            return result;
        }

        public T waitGet(long timeout, TimeUnit unit) {
            try {
                if (!latch.await(timeout, unit)) {
                    if (INVOKE_ON_EDT_TIMEOUT_LOG) new Throwable("SyncCallable timed out").printStackTrace();
                    return defaultValue;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return get();
        }
    }
}
