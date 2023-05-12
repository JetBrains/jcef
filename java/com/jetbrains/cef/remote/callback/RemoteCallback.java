package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefCallback;

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

