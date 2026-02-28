package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.callback.RemoteCompletionCallback;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefCompletionCallback;
import org.cef.handler.CefRequestContextHandler;
import org.cef.misc.Delayed;

public class RemoteRequestContext extends CefRequestContext {
    private final Delayed myDelayed;
    private final CefRequestContextHandler myHandler;
    private RemoteRequestContextHandler myRemoteWrapper;
    private int myBid = -1;
    private RpcContext myRpc;

    // Creates wrapper for global CefRequestContext instance
    public RemoteRequestContext(CefServer server) {
        myHandler = null;
        myDelayed = new Delayed("RemoteRequestContext(global)");
        myDelayed.finishOnConnection(server, () -> myRpc = server.getRpcContext());
    }

    // Creates wrapper for browser's CefRequestContext instance (can be 'global' too)
    public RemoteRequestContext(CefServer server, CefRequestContextHandler handler) {
        myHandler = handler;
        myDelayed = new Delayed("RemoteRequestContext(browser)");
        myDelayed.finishOnConnection(server, () -> myRpc = server.getRpcContext());
    }

    // Will be called for browser's instance immediately after bid obtained.
    public void setBid(int bid, RpcContext ctx) {
        assert bid >= 0;
        myBid = bid;
        myRpc = ctx;// bid is obtained => server is connected
        myDelayed.finishNow();
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
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).thriftId() : new RObject();
        myDelayed.runOrSchedule(() -> myRpc.invokeLater(s -> s.RequestContext_ClearCertificateExceptions(myBid, cbId)), "ClearCertificateExceptions");
    }

    @Override
    public void CloseAllConnections(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).thriftId() : new RObject();
        myDelayed.runOrSchedule(() -> myRpc.invokeLater(s -> s.RequestContext_CloseAllConnections(myBid, cbId)), "CloseAllConnections");
    }
}
