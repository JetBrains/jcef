package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.callback.CefCompletionCallback;

// Created on java side when doing async request to server.
// Disposed (remove reference in factory) when onComplete executed.
public class RemoteCompletionCallback extends RemoteJavaObject<CefCompletionCallback> {
    public static final RemoteJavaObjectFactory<RemoteCompletionCallback> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteCompletionCallback create(CefCompletionCallback delegate) {
        return FACTORY.create((index)->new RemoteCompletionCallback(index, delegate));
    }

    private RemoteCompletionCallback(int id, CefCompletionCallback delegate) { super(id, delegate); }
}
