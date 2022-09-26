package org.cef.callback;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CefNativeAdaperMulti implements CefNative{
    private final ConcurrentHashMap<String, NativeItem> N_CefHandle = new ConcurrentHashMap<String, NativeItem>();

    @Override
    public void setNativeRef(String identifer, long nativeRef) {
        final NativeItem item = N_CefHandle.get(identifer);
        if (item == null) {
            N_CefHandle.put(identifer, new NativeItem(nativeRef));
            return;
        }

        item.lock.lock();
        try {
            item.nativePointer = nativeRef;
        } finally {
            item.lock.unlock();
        }
    }

    // TODO: remove usages in Java: reimplement all native methods to obtain CefRefPtr safely (via JNI and lock)
    @Override
    public long getNativeRef(String identifer) {
        final NativeItem item = N_CefHandle.get(identifer);
        return item == null ? 0 : item.nativePointer;
    }

    /**
     * Method is called by the native code. It locks the mutex (associated with native reference) and then
     * returns pointer. In native code user must create CefRefPtr (internally invokes pointer->AddRef) and then
     * release mutex (via setNativeRefUnlocking or unlock).
     *
     * @param identifer The name of the interface class (e.g. CefFocusHandler).
     * @return The stored reference value of the native code.
     */
    long lockAndGetNativeRef(String identifer) {
        NativeItem item = N_CefHandle.get(identifer);
        if (item == null) {
            item = new NativeItem(0);
            item.lock.lock();
            N_CefHandle.put(identifer, item);
            return 0;
        }
        item.lock.lock();
        return item.nativePointer;
    }

    /**
     * Method is called by the native code to unlock the mutex (associated with native reference).
     *
     * @param identifer The name of the interface class (e.g. CefFocusHandler).
     */
    void unlock(String identifer) {
        final NativeItem item = N_CefHandle.get(identifer);
        if (item != null) item.lock.unlock();
    }

    private static class NativeItem {
        long nativePointer;
        final Lock lock = new ReentrantLock();

        private NativeItem(long nativePointer) {
            this.nativePointer = nativePointer;
        }
    }
}
