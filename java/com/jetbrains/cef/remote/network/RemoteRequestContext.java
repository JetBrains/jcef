package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.callback.RemoteCompletionCallback;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefCompletionCallback;
import org.cef.handler.CefRequestContextHandler;
import org.cef.misc.CefLog;

import java.util.ArrayList;
import java.util.List;

public class RemoteRequestContext extends CefRequestContext {
    private final List<Runnable> myDelayedActions = new ArrayList<>();
    private final CefRequestContextHandler myHandler;
    private RemoteRequestContextHandler myRemoteWrapper;
    private int myBid = -1;
    private RpcExecutor myRpc;

    // Creates wrapper for global CefRequestContext instance
    public RemoteRequestContext() {
        myHandler = null;
        CefServer.instance().onConnected(()->{
            synchronized (myDelayedActions) {
                myRpc = CefServer.instance().getService();
                myDelayedActions.forEach(r -> r.run());
                myDelayedActions.clear();
            }
        }, "RemoteRequestContext(GLOBAL)", true);
    }

    // Creates wrapper for browser's CefRequestContext instance (can be 'global' too)
    public RemoteRequestContext(CefRequestContextHandler handler) {
        myHandler = handler;
    }

    // Will be called for browser's instance immediately after bid obtained.
    public void setBid(int bid) {
        assert bid >= 0;
        myBid = bid;
        synchronized (myDelayedActions) {
            myRpc = CefServer.instance().getService();// bid is obtained => server is connected
            myDelayedActions.forEach(r -> r.run());
            myDelayedActions.clear();
        }
    }

    private void execute(Runnable rpcCall, String name) {
        synchronized (myDelayedActions) {
            if (myRpc != null)
                rpcCall.run();
            else {
                CefLog.Debug("RemoteRequestContext: %s: add delayed action %s", this, name);
                myDelayedActions.add(rpcCall);
            }
        }
    }

    @Override
    public void dispose() {
        // Dispose handler object
        if (myRemoteWrapper != null)
            RemoteRequestContextHandler.FACTORY.dispose(myRemoteWrapper.getId());

        // Nothing to do with context object, remote server peer can be:
        //  1. native global CefRequestContext (lifetime is managed by cef)
        //  2. or owned by RemoteClientHandler (and has the same lifetime).
    }

    @Override
    public boolean isGlobal() {
        return myHandler == null;
    }

    @Override
    public CefRequestContextHandler getHandler() {
        return myHandler;
    }

    public RemoteRequestContextHandler getRemoteHandler() {
        if (myHandler == null)
            return null;

        if (myRemoteWrapper == null)
            myRemoteWrapper = RemoteRequestContextHandler.create(myHandler);
        return myRemoteWrapper;
    }

    @Override
    public void ClearCertificateExceptions(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).thriftId() : new RObject(-1);
        execute(() -> myRpc.exec(s -> s.RequestContext_ClearCertificateExceptions(myBid, cbId)), "ClearCertificateExceptions");
    }

    @Override
    public void CloseAllConnections(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).thriftId() : new RObject(-1);
        execute(() -> myRpc.exec(s -> s.RequestContext_CloseAllConnections(myBid, cbId)), "CloseAllConnections");
    }
}
