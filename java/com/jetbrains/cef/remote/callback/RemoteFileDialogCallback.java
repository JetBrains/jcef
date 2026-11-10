package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefFileDialogCallback;

import java.util.Vector;

public class RemoteFileDialogCallback extends RemoteServerObject implements CefFileDialogCallback {
    public RemoteFileDialogCallback(RpcContext rpcContext, RObject robj) {
        super(rpcContext, robj);
    }

    @Override
    public void Continue(Vector<String> filePaths) {
        myRpc.exec((s) -> s.FileDialogCallback_Continue(toRObject(), filePaths));
    }

    @Override
    public void Cancel() {
        myRpc.exec((s) -> s.FileDialogCallback_Cancel(toRObject()));
    }

    @Override
    public void flush() {}

    @Override
    protected void disposeOnServerImpl() {
        final RObject id = toRObject();
        myRpc.invokeLater(s -> s.FileDialogCallback_Dispose(id));
    }
}