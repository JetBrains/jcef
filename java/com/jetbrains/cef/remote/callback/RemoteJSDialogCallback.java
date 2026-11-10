package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefJSDialogCallback;

public class RemoteJSDialogCallback extends RemoteServerObject implements CefJSDialogCallback {
    public RemoteJSDialogCallback(RpcContext rpcContext, RObject robj) {
        super(rpcContext, robj);
    }

    @Override
    public void Continue(boolean success, String user_input) {
        myRpc.exec((s) -> s.JSDialogCallback_Continue(toRObject(), success, user_input));
    }

    @Override
    public void flush() {}

    @Override
    protected void disposeOnServerImpl() {
        final RObject id = toRObject();
        myRpc.invokeLater(s -> s.JSDialogCallback_Dispose(id));
    }
}