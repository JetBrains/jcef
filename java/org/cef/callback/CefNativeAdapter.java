package org.cef.callback;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CefNativeAdapter implements CefNative {
    // Used internally to store a pointer to the CEF object.
    private long N_CefHandle = 0;
    private final Lock lock = new ReentrantLock();

    // TODO: remove usages in Java: reimplement all native methods to obtain CefRefPtr safely (via JNI and lock)
    @Override
    public long getNativeRef(String identifer) { return N_CefHandle; }

    public long getNativeRef() {
        return N_CefHandle;
    }

    @Override
    public long setNativeRef(String identifer, long nativeRef) {
        lock.lock();
        try {
            long prev = N_CefHandle;
            N_CefHandle = nativeRef;
            return prev;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long lockAndGetNativeRef(String identifer) {
        lock.lock();
        return N_CefHandle;
    }

    @Override
    public void unlock(String identifer) {
        lock.unlock();
    }
}
