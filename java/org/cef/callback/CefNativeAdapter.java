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
    public void setNativeRef(String identifer, long nativeRef) {
        N_CefHandle = nativeRef;
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
        lock.lock();
        return N_CefHandle;
    }

    /**
     * Method is called by the native code to unlock the mutex (associated with native reference).
     *
     * @param identifer The name of the interface class (e.g. CefFocusHandler).
     */
    void unlock(String identifer) {
        lock.unlock();
    }
}
