package org.cef.jdk;

import org.cef.OS;

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
            // currently the mechanism is actual on macOS only
            return OS.isMacintosh() && mInvoke != null;
        }
    }
}
