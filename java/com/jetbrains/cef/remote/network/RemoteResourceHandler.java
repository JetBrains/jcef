package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.handler.CefResourceHandler;

// Created on java side by server request.
// Lifetime if managed by server.
// Disposed (remove reference in factory) by server request.
public class RemoteResourceHandler extends RemoteJavaObject<CefResourceHandler> {
    public static final RemoteJavaObjectFactory<RemoteResourceHandler> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteResourceHandler create(CefResourceHandler delegate) {
        return FACTORY.create((index)->new RemoteResourceHandler(index, delegate));
    }

    private RemoteResourceHandler(int id, CefResourceHandler delegate) { super(id, delegate); }
}
