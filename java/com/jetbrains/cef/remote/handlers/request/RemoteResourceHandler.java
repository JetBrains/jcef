package com.jetbrains.cef.remote.handlers.request;

import com.jetbrains.cef.remote.handlers.RemoteJavaObject;
import com.jetbrains.cef.remote.handlers.RemoteObjectFactory;
import org.cef.handler.CefResourceHandler;

public class RemoteResourceHandler extends RemoteJavaObject<CefResourceHandler> {
    public static final RemoteObjectFactory<RemoteResourceHandler> FACTORY = new RemoteObjectFactory<>();

    public static RemoteResourceHandler create(CefResourceHandler delegate) {
        return FACTORY.create((index)->new RemoteResourceHandler(index, delegate));
    }

    private RemoteResourceHandler(int id, CefResourceHandler delegate) { super(id, delegate); }
}
