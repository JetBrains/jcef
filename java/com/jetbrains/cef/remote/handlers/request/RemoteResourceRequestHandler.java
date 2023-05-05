package com.jetbrains.cef.remote.handlers.request;

import com.jetbrains.cef.remote.handlers.RemoteJavaObject;
import com.jetbrains.cef.remote.handlers.RemoteObjectFactory;
import org.cef.handler.CefResourceRequestHandler;

public class RemoteResourceRequestHandler extends RemoteJavaObject<CefResourceRequestHandler> {
    public static final RemoteObjectFactory<RemoteResourceRequestHandler> FACTORY = new RemoteObjectFactory<>();

    private RemoteResourceRequestHandler(int id, CefResourceRequestHandler delegate) { super(id, delegate); }

    public static RemoteResourceRequestHandler create(CefResourceRequestHandler delegate) {
        return FACTORY.create((index)->new RemoteResourceRequestHandler(index, delegate));
    }
}
