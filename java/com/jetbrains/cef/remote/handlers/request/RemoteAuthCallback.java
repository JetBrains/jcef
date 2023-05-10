package com.jetbrains.cef.remote.handlers.request;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.handlers.RemoteServerObject;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import com.jetbrains.cef.remote.thrift_codegen.Server;
import org.apache.thrift.TException;
import org.cef.callback.CefAuthCallback;

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
