package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift.transport.TTransportException;

public class RpcContext {
    public final RpcExecutor main = new RpcExecutor();
    public final RpcExecutor bg = new RpcExecutor(); // TODO: schedule bg calls

    public void openTransport() throws TTransportException {
        main.openTransport();
        bg.openTransport();
    }
    public void closeTransport() {
        main.closeTransport();
        bg.closeTransport();
    }
}
