package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import org.cef.callback.CefRunFileDialogCallback;

// Created on java side to get (async) result (from file dialog) from server.
// Disposed (remove reference in factory) immediately after delegate.onFileDialogDismissed is executed.
public class RemoteRunFileDialogCallback extends RemoteJavaObject<CefRunFileDialogCallback> {
    public static final RemoteJavaObjectFactory<RemoteRunFileDialogCallback> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteRunFileDialogCallback create(CefRunFileDialogCallback delegate) {
        return FACTORY.create((index)->new RemoteRunFileDialogCallback(index, delegate));
    }

    private RemoteRunFileDialogCallback(int id, CefRunFileDialogCallback delegate) { super(id, delegate); }
}
