package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.handler.CefRequestContextHandler;

// Created on java side in the first call RemoteRequestContext.getRemoteHandler (when going to create browser).
// Owned by RemoteRequestContext (lifetime if managed by owner).
public class RemoteRequestContextHandler extends RemoteJavaObject<CefRequestContextHandler> {
    public static final RemoteJavaObjectFactory<RemoteRequestContextHandler> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteRequestContextHandler create(CefRequestContextHandler delegate) {
        return FACTORY.create((index)->new RemoteRequestContextHandler(index, delegate));
    }

    private RemoteRequestContextHandler(int id, CefRequestContextHandler delegate) { super(id, delegate); }
}

