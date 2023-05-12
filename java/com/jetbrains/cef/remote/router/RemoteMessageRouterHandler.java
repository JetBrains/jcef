package com.jetbrains.cef.remote.router;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.handler.CefMessageRouterHandler;

// 1. Represent remote java peer for native server object (CefMessageRouterHandler).
// 2. Created on java side when user adds CefMessageRouterHandler into RemoteMessageRouter.
// 3. Lifetime if managed by java owner (RemoteMessageRouter): disposed in removeHandler (or
// when RemoteMessageRouter finalizes)
public class RemoteMessageRouterHandler extends RemoteJavaObject<CefMessageRouterHandler> {
    public static final RemoteJavaObjectFactory<RemoteMessageRouterHandler> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteMessageRouterHandler create(CefMessageRouterHandler delegate) {
        return FACTORY.create((index)->new RemoteMessageRouterHandler(index, delegate));
    }

    public static RemoteMessageRouterHandler findByDelegate(CefMessageRouterHandler delegate) {
        return FACTORY.find((item)->item.getDelegate() == delegate);
    }

    private RemoteMessageRouterHandler(int id, CefMessageRouterHandler delegate) { super(id, delegate); }
}