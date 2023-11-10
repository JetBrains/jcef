package com.jetbrains.cef.remote.router;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefQueryCallback;

// 1. Represent remote java peer for native server object (CefQueryCallback) that
// valid in any context (destroyed on server manually, via rpc from java side).
// 2. Created on java side when processing some server request.
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc.
public class RemoteQueryCallback extends RemoteServerObject implements CefQueryCallback {
    public RemoteQueryCallback(RpcExecutor server, RObject robj) {
        super(server, robj);
    }

    @Override
    public void flush() {}

    @Override
    protected void disposeOnServerImpl() {
        // NOTE: server object will be disposed after Continue or Cancel invocations.
        // But if callback wasn't used we should dispose server object here
        myServer.exec((s)-> s.QueryCallback_Dispose(thriftId()));
    }

    @Override
    public void success(String response) {
        myServer.exec((s)-> s.QueryCallback_Success(thriftId(), response));
    }

    @Override
    public void failure(int error_code, String error_message) {
        myServer.exec((s)-> s.QueryCallback_Failure(thriftId(), error_code, error_message));
    }
}
