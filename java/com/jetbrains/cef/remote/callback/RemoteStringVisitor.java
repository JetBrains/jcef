package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.callback.CefStringVisitor;

// Created on java side to get (async) strings from server.
// Disposed by server request.
public class RemoteStringVisitor extends RemoteJavaObject<CefStringVisitor> {
    public static final RemoteJavaObjectFactory<RemoteStringVisitor> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteStringVisitor create(CefStringVisitor delegate) {
        return FACTORY.create((index)->new RemoteStringVisitor(index, delegate));
    }

    private RemoteStringVisitor(int id, CefStringVisitor delegate) { super(id, delegate); }
}
