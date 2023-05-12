package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.handler.CefCookieAccessFilter;

// Created on java side by server request.
// Lifetime if managed by server.
// Disposed (remove reference in factory) by server request.
public class RemoteCookieAccessFilter extends RemoteJavaObject<CefCookieAccessFilter> {
    public static final RemoteJavaObjectFactory<RemoteCookieAccessFilter> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteCookieAccessFilter create(CefCookieAccessFilter delegate) {
        return FACTORY.create((index)->new RemoteCookieAccessFilter(index, delegate));
    }

    private RemoteCookieAccessFilter(int id, CefCookieAccessFilter delegate) { super(id, delegate); }
}
