package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.callback.RemoteCompletionCallback;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefCookieVisitor;
import org.cef.misc.CefLog;
import org.cef.network.CefCookie;

// 1. Represent remote java peer for native server object (CefCookieManager) that is
// valid in any context (destroyed on server manually, via rpc from java side).
// 2. Created on java side when user created instance of RemoteCookieManager (and connection was established).
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc.
public class RemoteCookieManagerImpl extends RemoteServerObject {
    private RemoteCookieManagerImpl(RpcContext rpcContext, RObject robj) {
        super(rpcContext, robj);
    }

    public static RemoteCookieManagerImpl create(RpcContext rpcContext) {
        RObject robj = rpcContext.execObj(s->s.CookieManager_Create());
        if (robj.isNull) {
            CefLog.Error("CookieManager_Create returns invalid uid %d.", robj.uid);
            return null;
        }
        return new RemoteCookieManagerImpl(rpcContext, robj);
    }

    @Override
    protected void disposeOnServerImpl() {
        final RObject id = toRObject();
        myRpc.invokeLater(s -> s.CookieManager_Dispose(id));
    }

    @Override
    public void flush() {
        // Nothing to do (CefCookieManager hasn't any cache items)
    }

    public boolean visitAllCookies(CefCookieVisitor visitor) {
        if (visitor == null)
            return false;
        RemoteCookieVisitor rvisitor = RemoteCookieVisitor.create(visitor);
        return myRpc.execObj(s -> s.CookieManager_VisitAllCookies(toRObject(), rvisitor.toRObject()));
    }

    public boolean visitUrlCookies(String url, boolean includeHttpOnly, CefCookieVisitor visitor) {
        if (visitor == null)
            return false;
        RemoteCookieVisitor rvisitor = RemoteCookieVisitor.create(visitor);
        return myRpc.execObj(s -> s.CookieManager_VisitUrlCookies(toRObject(), rvisitor.toRObject(), url, includeHttpOnly));
    }

    public boolean setCookie(String url, CefCookie cookie) {
        return myRpc.execObj(s -> s.CookieManager_SetCookie(toRObject(), url, RemoteCookieManager.toThriftCookie(cookie)));
    }

    public boolean deleteCookies(String url, String cookieName) {
        return myRpc.execObj(s -> s.CookieManager_DeleteCookies(toRObject(), url, cookieName));
    }

    public boolean flushStore(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).toRObject() : new RObject();
        return myRpc.execObj(s -> s.CookieManager_FlushStore(toRObject(), cbId));
    }
}
