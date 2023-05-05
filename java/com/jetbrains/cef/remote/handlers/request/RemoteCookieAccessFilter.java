package com.jetbrains.cef.remote.handlers.request;

import com.jetbrains.cef.remote.handlers.RemoteJavaObject;
import com.jetbrains.cef.remote.handlers.RemoteObjectFactory;
import org.cef.handler.CefCookieAccessFilter;

public class RemoteCookieAccessFilter extends RemoteJavaObject<CefCookieAccessFilter> {
    public static final RemoteObjectFactory<RemoteCookieAccessFilter> FACTORY = new RemoteObjectFactory<>();

    public static RemoteCookieAccessFilter create(CefCookieAccessFilter delegate) {
        return FACTORY.create((index)->new RemoteCookieAccessFilter(index, delegate));
    }

    private RemoteCookieAccessFilter(int id, CefCookieAccessFilter delegate) { super(id, delegate); }
}
