package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.RObject;

// Used to invoke methods of java delegate from native server object.
// Stores ref to java delegate.
// Created via RemoteJavaObjectFactory
// Should be manually disposed via RemoteJavaObjectFactory.
public class RemoteJavaObject<T> {
    private final int myId;
    private final T myDelegate;

    protected RemoteJavaObject(int id, T delegate) {
        myId = id;
        myDelegate = delegate;
    }

    public int getId() { return myId; }
    public T getDelegate() { return myDelegate; }

    public RObject thriftId() {
        RObject result = new RObject(myId);
        return result;
    }
    public RObject thriftId(int flags) {
        RObject result = new RObject(myId);
        result.setFlags(flags);
        return result;
    }
}
