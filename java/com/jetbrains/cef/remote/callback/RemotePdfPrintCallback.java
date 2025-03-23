package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.callback.CefPdfPrintCallback;

// Created on java side to get (async) result from server.
// Disposed (remove reference in factory) immediately after delegate.onPdfPrintFinished is executed.
public class RemotePdfPrintCallback extends RemoteJavaObject<CefPdfPrintCallback> {
    public static final RemoteJavaObjectFactory<RemotePdfPrintCallback> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemotePdfPrintCallback create(CefPdfPrintCallback delegate) {
        if (delegate == null)
            return null;
        return FACTORY.create((index)->new RemotePdfPrintCallback(index, delegate));
    }

    private RemotePdfPrintCallback(int id, CefPdfPrintCallback delegate) { super(id, delegate); }
}

