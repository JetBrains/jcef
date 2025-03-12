package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefRunContextMenuCallback;

public class RemoteRunContextMenuCallback extends RemoteServerObject implements CefRunContextMenuCallback {
    public RemoteRunContextMenuCallback(RpcContext rpcContext, RObject robj) {
        super(rpcContext, robj);
    }

    @Override
    protected void disposeOnServerImpl() {
        final RObject id = thriftId();
        myRpc.invokeLater(s -> s.CefRunContextMenuCallback_Dispose(id));
    }

    @Override
    public void flush() {
    }

    @Override
    public void Continue(int selected_command_id, int event_flags) {
        myRpc.exec((s) -> s.CefRunContextMenuCallback_Continue(thriftId(), selected_command_id, event_flags));
    }

    @Override
    public void cancel() {
        myRpc.exec((s) -> s.CefRunContextMenuCallback_Cancel(thriftId()));
    }
}
