package com.jetbrains.cef.remote.handlers.request;

import com.jetbrains.cef.remote.handlers.RemoteServerObject;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import com.jetbrains.cef.remote.thrift_codegen.Server;
import org.apache.thrift.TException;
import org.cef.callback.CefCallback;

public class RemoteCallback extends RemoteServerObject implements CefCallback {
    public RemoteCallback(Server.Client server, RObject robj) {
        super(server, robj);
    }

    @Override
    public void flush() {}

    @Override
    protected void disposeOnServerImpl() {
        try {
            // NOTE: server object will be disposed after Continue or Cancel invocations.
            // But if callback wasn't used we should dispose server object here
            myServer.Callback_Dispose(thriftId());
        } catch (TException e) {
            onThriftException(e);
        }
    }

    @Override
    public void Continue() {
        try {
            // NOTE: server object will be disposed after this call
            myServer.Callback_Continue(thriftId());
        } catch (TException e) {
            onThriftException(e);
        }
    }

    @Override
    public void cancel() {
        try {
            // NOTE: server object will be disposed after this call
            myServer.Callback_Cancel(thriftId());
        } catch (TException e) {
            onThriftException(e);
        }
    }
}

