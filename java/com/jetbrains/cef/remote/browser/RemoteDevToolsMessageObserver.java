package com.jetbrains.cef.remote.browser;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.browser.CefDevToolsMessageObserver;

public class RemoteDevToolsMessageObserver extends RemoteJavaObject<CefDevToolsMessageObserver> {
    public static final RemoteJavaObjectFactory<RemoteDevToolsMessageObserver> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteDevToolsMessageObserver create(CefDevToolsMessageObserver delegate) {
        return FACTORY.create((index)->new RemoteDevToolsMessageObserver(index, delegate));
    }

    private RemoteDevToolsMessageObserver(int id, CefDevToolsMessageObserver delegate) { super(id, delegate); }
}
