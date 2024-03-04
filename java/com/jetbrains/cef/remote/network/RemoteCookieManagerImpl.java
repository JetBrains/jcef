package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.callback.RemoteCompletionCallback;
import com.jetbrains.cef.remote.thrift_codegen.Cookie;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefCookieVisitor;
import org.cef.misc.BoolRef;
import org.cef.misc.CefLog;
import org.cef.network.CefCookie;

import java.util.Set;

// 1. Represent remote java peer for native server object (CefCookieManager) that is
// valid in any context (destroyed on server manually, via rpc from java side).
// 2. Created on java side when user created instance of RemoteCookieManager (and connection was established).
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc.
public class RemoteCookieManagerImpl extends RemoteServerObject {
    private RemoteCookieManagerImpl(RpcExecutor server, RObject robj) {
        super(server, robj);
    }

    public static RemoteCookieManagerImpl create(RpcExecutor server) {
        RObject robj = server.execObj(s->s.CookieManager_Create());
        if (robj.objId < 0) {
            CefLog.Error("CookieManager_Create returns invalid objId %d.", robj.objId);
            return null;
        }
        return new RemoteCookieManagerImpl(server, robj);
    }

    @Override
    protected void disposeOnServerImpl() {
        myServer.exec((s)->s.CookieManager_Dispose(thriftId()));
    }

    @Override
    public void flush() {
        // Nothing to do (CefCookieManager hasn't any cache items)
    }

    public boolean visitAllCookies(CefCookieVisitor visitor) {
        if (visitor == null)
            return false;
        RemoteCookieVisitor rvisitor = RemoteCookieVisitor.create(visitor);
        return myServer.execObj(s -> s.CookieManager_VisitAllCookies(thriftId(), rvisitor.thriftId()));
    }

    public boolean visitUrlCookies(String url, boolean includeHttpOnly, CefCookieVisitor visitor) {
        if (visitor == null)
            return false;
        RemoteCookieVisitor rvisitor = RemoteCookieVisitor.create(visitor);
        return myServer.execObj(s -> s.CookieManager_VisitUrlCookies(thriftId(), rvisitor.thriftId(), url, includeHttpOnly));
    }

    public boolean setCookie(String url, CefCookie cookie) {
        return myServer.execObj(s -> s.CookieManager_SetCookie(thriftId(), url, RemoteCookieManager.toThriftCookie(cookie)));
    }

    public boolean deleteCookies(String url, String cookieName) {
        return myServer.execObj(s -> s.CookieManager_DeleteCookies(thriftId(), url, cookieName));
    }

    public boolean flushStore(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).thriftId() : new RObject(-1);
        return myServer.execObj(s -> s.CookieManager_FlushStore(thriftId(), cbId));
    }
}
