package com.jetbrains.cef.remote.router;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteObjectFactory;
import org.cef.handler.CefMessageRouterHandler;


public class RemoteMessageRouterHandler extends RemoteJavaObject<CefMessageRouterHandler> {
    public static final RemoteObjectFactory<RemoteMessageRouterHandler> FACTORY = new RemoteObjectFactory<>();

    public static RemoteMessageRouterHandler create(CefMessageRouterHandler delegate) {
        return FACTORY.create((index)->new RemoteMessageRouterHandler(index, delegate));
    }

    public static RemoteMessageRouterHandler find(CefMessageRouterHandler delegate, boolean create) {
        RemoteMessageRouterHandler result = FACTORY.find((item)->item.getDelegate() == delegate);
        if (create && result == null)
            result = create(delegate);
        return result;
    }

    private RemoteMessageRouterHandler(int id, CefMessageRouterHandler delegate) { super(id, delegate); }
}