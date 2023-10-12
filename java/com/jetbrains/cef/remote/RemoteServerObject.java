package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.RObject;

// 1. Direct inheritors represent remote java peer for native server object that
// valid in any context (destroyed on server manually, via rpc from java side).
// 2. Created on java side when processing some server request or when
// user for example configures RemoteMessageRouter.
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc (see finalize and disposeOnServer).
// Also the server object can be disposed manually (via disposeOnServer). After that
// moment all requests from java to native will return errors (or default values).
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
