package com.jetbrains.cef.remote.callback;

import com.jetbrains.cef.remote.RemoteJavaObject;
import com.jetbrains.cef.remote.RemoteJavaObjectFactory;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefCompletionCallback;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// Created on java side when doing async request to server.
// Disposed (remove reference in factory) immediately after delegate.onComplete is executed.
public class RemoteIntCallback extends RemoteJavaObject<Consumer<Integer>> {
    public static final RemoteJavaObjectFactory<RemoteIntCallback> FACTORY = new RemoteJavaObjectFactory<>();

    public static RemoteIntCallback create(Consumer<Integer> delegate) {
        return FACTORY.create((index)->new RemoteIntCallback(index, delegate));
    }

    public void onComplete(int result) {
        getDelegate().accept(result);
    }

    private RemoteIntCallback(int id, Consumer<Integer> delegate) { super(id, delegate); }
}
