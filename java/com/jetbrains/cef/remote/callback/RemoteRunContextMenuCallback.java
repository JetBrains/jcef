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
        myRpc.bg.exec((s) -> s.CefRunContextMenuCallback_Dispose(thriftId()));
    }

    @Override
    public void flush() {
    }

    @Override
    public void Continue(int selected_command_id, int event_flags) {
        myRpc.main.exec((s) -> s.CefRunContextMenuCallback_Continue(thriftId(), selected_command_id, event_flags));
    }

    @Override
    public void cancel() {
        myRpc.main.exec((s) -> s.CefRunContextMenuCallback_Cancel(thriftId()));
    }
}
