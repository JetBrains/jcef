package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.thrift_codegen.Cookie;
import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefCookieVisitor;
import org.cef.network.CefCookie;
import org.cef.network.CefCookieManager;

import java.util.Date;

public class RemoteCookieManager extends CefCookieManager {
    RemoteCookieManagerImpl myImpl;
    private volatile boolean myIsDisposed = false;

    private RemoteCookieManager() {
        CefServer.instance().onConnected(()->{
            myImpl = RemoteCookieManagerImpl.create(CefServer.instance().getService());
        }, "CookieManager_Create", true);
    }

    public static RemoteCookieManager createGlobal() {
        return new RemoteCookieManager();
    }

    @Override
    public void dispose() {
        myIsDisposed = true;
    }

    @Override
    public boolean visitAllCookies(CefCookieVisitor visitor) {
        if (myImpl != null) {
            if (myIsDisposed)
                return false;
            return myImpl.visitAllCookies(visitor);
        }

        throw new RuntimeException("visitAllCookies: connection with cef_server wasn't established yet. Consider subscribing on JCEF initialisation(see CefApp#onInitialization)");
    }

    @Override
    public boolean visitUrlCookies(String url, boolean includeHttpOnly, CefCookieVisitor visitor) {
        if (myImpl != null) {
            if (myIsDisposed)
                return false;
            return myImpl.visitUrlCookies(url, includeHttpOnly, visitor);
        }

        throw new RuntimeException("visitUrlCookies: connection with cef_server wasn't established yet. Consider subscribing on JCEF initialisation(see CefApp#onInitialization)");
    }

    @Override
    public boolean setCookie(String url, CefCookie cookie) {
        if (myImpl != null) {
            if (myIsDisposed)
                return false;
            return myImpl.setCookie(url, cookie);
        }

        throw new RuntimeException("setCookie: connection with cef_server wasn't established yet. Consider subscribing on JCEF initialisation(see CefApp#onInitialization)");
    }

    @Override
    public boolean deleteCookies(String url, String cookieName) {
        if (myImpl != null) {
            if (myIsDisposed)
                return false;
            return myImpl.deleteCookies(url, cookieName);
        }

        throw new RuntimeException("deleteCookies: connection with cef_server wasn't established yet. Consider subscribing on JCEF initialisation(see CefApp#onInitialization)");
    }

    @Override
    public boolean flushStore(CefCompletionCallback handler) {
        if (myImpl != null) {
            if (myIsDisposed)
                return false;
            return myImpl.flushStore(handler);
        }

        throw new RuntimeException("flushStore: connection with cef_server wasn't established yet. Consider subscribing on JCEF initialisation(see CefApp#onInitialization)");
    }

    public static CefCookie toCefCookie(Cookie c) {
        Date creation = c.creation == 0 ? null : new Date(c.creation);
        Date lastAccess = c.lastAccess == 0 ? null : new Date(c.lastAccess);
        Date expires = c.isSetExpires() ? new Date(c.expires) : null;
        return new CefCookie(c.name, c.value, c.domain, c.path, c.secure, c.httponly, creation, lastAccess, expires != null, expires);
    }

    public static Cookie toThriftCookie(CefCookie c) {
        Cookie cookie = new Cookie(c.name, c.value, c.domain, c.path, c.secure, c.httponly, c.creation == null ? 0 : c.creation.getTime(), c.lastAccess == null ? 0 : c.lastAccess.getTime());
        if (c.hasExpires && c.expires != null)
            cookie.setExpires(c.expires.getTime());
        return cookie;
    }
}
