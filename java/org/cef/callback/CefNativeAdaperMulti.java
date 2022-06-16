package org.cef.callback;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CefNativeAdaperMulti implements CefNative{
    private final ConcurrentHashMap<String, NativeItem> N_CefHandle = new ConcurrentHashMap<String, NativeItem>();

    @Override
    public long setNativeRef(String identifer, long nativeRef) {
        final NativeItem item = N_CefHandle.get(identifer);
        if (item == null) {
            N_CefHandle.put(identifer, new NativeItem(nativeRef));
            return 0;
        }

        item.lock.lock();
        try {
            long prev = item.nativePointer;
            item.nativePointer = nativeRef;
            return prev;
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

    @Override
    public long lockAndGetNativeRef(String identifer) {
        final NativeItem item = N_CefHandle.get(identifer);
        if (item == null) return 0;
        item.lock.lock();
        return item.nativePointer;
    }

    @Override
    public void unlock(String identifer) {
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
