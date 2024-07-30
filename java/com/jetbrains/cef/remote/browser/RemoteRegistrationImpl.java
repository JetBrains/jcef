package com.jetbrains.cef.remote.browser;

import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.thrift_codegen.RObject;

// 1. Represent remote java peer for native server object (CefRegistration) that is
// valid in any context (destroyed on server via rpc from java side).
// 2. Created on java side from received RObject (see RemoteBrowser.addDevToolsMessageObserver)
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc (or when user invokes dispose())
public class RemoteRegistrationImpl extends RemoteServerObject {
    public RemoteRegistrationImpl(RpcExecutor server, RObject robj) {
        super(server, robj);
    }

    @Override
    protected void disposeOnServerImpl() {
        myServer.exec((s)-> s.Registration_Dispose(thriftId()));
    }

    @Override
    public void flush() {} // No cache, nothing to do
}