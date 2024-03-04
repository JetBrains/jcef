package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.callback.CefCookieVisitor;

// Created on java side when doing async cookie visiting.
// Disposed (remove reference in factory):
//  1. when the last cookie has been visited.
//  2. when Visitor returns false (i.e. stop traverse over cookies)
//  3. by server request (from CefRefPtr<RemoteCookieVisitor> dtor)
public class RemoteCookieVisitor extends RemoteJavaObject<CefCookieVisitor> {
    public static final RemoteJavaObjectFactory<RemoteCookieVisitor> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteCookieVisitor create(CefCookieVisitor delegate) {
        return FACTORY.create((index)->new RemoteCookieVisitor(index, delegate));
    }

    private RemoteCookieVisitor(int id, CefCookieVisitor delegate) { super(id, delegate); }
}

