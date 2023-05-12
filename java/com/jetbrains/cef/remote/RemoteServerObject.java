package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.RObject;

//
// Represents server object that valid in any context (destroyed on server manually, via rpc from java side)
//
public abstract class RemoteServerObject extends RemoteServerObjectLocal {
    private Object myDisposeMutex = new Object();
    private boolean myIsDisposed = false;

    public RemoteServerObject(RpcExecutor server, RObject robj) {
        super(server, robj);
    }

    @Override
    protected void finalize() throws Throwable {
        disposeOnServer();
        super.finalize();
    }

    public final void disposeOnServer() {
        synchronized (myDisposeMutex) {
            if (!myIsDisposed) {
                disposeOnServerImpl();
                myIsDisposed = true;
            }
        }
    }

    // Destroys remote peer on server side.
    protected abstract void disposeOnServerImpl();
}
