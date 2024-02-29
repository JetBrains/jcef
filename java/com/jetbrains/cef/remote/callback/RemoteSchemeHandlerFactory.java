package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.callback.CefSchemeHandlerFactory;

// Created on java side and remains alive until ClearAllSchemeHandlersFactories called.
// In general, lifetime should be managed by server and object should be disposed (remove reference in factory) by server request (that should be called inside ClearAllSchemeHandlersFactories).
public class RemoteSchemeHandlerFactory extends RemoteJavaObject<CefSchemeHandlerFactory> {
    public static final RemoteJavaObjectFactory<RemoteSchemeHandlerFactory> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteSchemeHandlerFactory create(CefSchemeHandlerFactory delegate) {
        return FACTORY.create((index)->new RemoteSchemeHandlerFactory(index, delegate));
    }

    private RemoteSchemeHandlerFactory(int id, CefSchemeHandlerFactory delegate) { super(id, delegate); }
}