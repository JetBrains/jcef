package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefAuthCallback;

// 1. Represent remote java peer for native server object (CefAuthCallback) that
// valid in any context (destroyed on server manually, via rpc from java side).
// 2. Created on java side when processing some server request.
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc.
public class RemoteAuthCallback extends RemoteServerObject implements CefAuthCallback {
    public RemoteAuthCallback(RpcExecutor server, RObject robj) {
        super(server, robj);
    }

    @Override
    public void flush() {}

    @Override
    protected void disposeOnServerImpl() {
        // NOTE: server object will be disposed after Continue or Cancel invocations.
        // But if callback wasn't used we should dispose server object here
        myServer.exec((s)-> s.AuthCallback_Dispose(thriftId()));
    }

    @Override
    public void Continue(String username, String password) {
        // NOTE: server object will be disposed after this call
        myServer.exec((s)-> s.AuthCallback_Continue(thriftId(), username, password));
    }

    @Override
    public void cancel() {
        // NOTE: server object will be disposed after this call
        myServer.exec((s)-> s.AuthCallback_Cancel(thriftId()));
    }
}
