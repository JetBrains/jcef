package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefCallback;

// 1. Represent remote java peer for native server object (CefCallback) that
// valid in any context (destroyed on server manually, via rpc from java side).
// 2. Created on java side when processing some server request.
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc.
public class RemoteCallback extends RemoteServerObject implements CefCallback {
    public RemoteCallback(RpcExecutor server, RObject robj) {
        super(server, robj);
    }

    @Override
    public void flush() {}

    @Override
    protected void disposeOnServerImpl() {
        // NOTE: server object will be disposed after Continue or Cancel invocations.
        // But if callback wasn't used we should dispose server object here
        myServer.exec((s)-> s.Callback_Dispose(thriftId()));
    }

    @Override
    public void Continue() {
        // NOTE: server object will be disposed after this call
        myServer.exec((s)-> s.Callback_Continue(thriftId()));
    }

    @Override
    public void cancel() {
        // NOTE: server object will be disposed after this call
        myServer.exec((s)-> s.Callback_Cancel(thriftId()));
    }
}

