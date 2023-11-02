package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.handler.CefResourceRequestHandler;

// Created on java side by server request.
// Lifetime if managed by server.
// Disposed (remove reference in factory) by server request.
public class RemoteResourceRequestHandler extends RemoteJavaObject<CefResourceRequestHandler> {
    public static final RemoteJavaObjectFactory<RemoteResourceRequestHandler> FACTORY = new RemoteJavaObjectFactory<>();

    private RemoteResourceRequestHandler(int id, CefResourceRequestHandler delegate) { super(id, delegate); }

    public static RemoteResourceRequestHandler create(CefResourceRequestHandler delegate) {
        return FACTORY.create((index)->new RemoteResourceRequestHandler(index, delegate));
    }
}
