package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.CefClient;
import org.cef.browser.CefFrame;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;

// 1. Represents remote java peer for native server object (CefFrame).
// 2. Created on java side when processing some server request
// 3. Lifetime of remote native peer if managed by server: native object
// peer (CefFrame) is destroyed immediately after rpc finished. After that
// moment all requests from java to native will return errors (or default values).
// Java object will be destroyed via usual gc.
public class RemoteFrame extends RemoteServerObjectLocal implements CefFrame {
    public RemoteFrame(RpcExecutor server, RObject frame) {
        super(server, frame);
    }

    @Override
    public void flush() {
        // Nothing to do (CefFrame is read-only object).
    }

    @Override
    public void dispose() {
        // Nothing to do (lifetime of remote native peer if managed by server). Java object will be destroyed via usual gc.
    }

    @Override
    public String getIdentifier() {
        return myCache.get("Identifier");
    }

    @Override
    public String getURL() {
        return myCache.get("URL");
    }

    @Override
    public String getName() {
        return myCache.get("Name");
    }

    @Override
    public boolean isMain() {
        return getBoolVal("IsMain");
    }

    @Override
    public boolean isValid() {
        return getBoolVal("IsValid");
    }

    @Override
    public boolean isFocused() {
        return getBoolVal("IsFocused");
    }

    @Override
    public CefFrame getParent() {
        CefLog.Error("TODO: implement getParent().");
        return null;
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        myServer.exec((s)->{
            s.Frame_ExecuteJavaScript(myId, code, url, line);
        });
    }

    @Override
    public void undo() {
        CefLog.Error("TODO: implement undo().");
    }

    @Override
    public void redo() {
        CefLog.Error("TODO: implement redo().");
    }

    @Override
    public void cut() {
        CefLog.Error("TODO: implement cut().");
    }

    @Override
    public void copy() {
        CefLog.Error("TODO: implement copy().");
    }

    @Override
    public void paste() {
        CefLog.Error("TODO: implement undo().");
    }

    @Override
    public void delete() {
        CefLog.Error("TODO: implement delete().");
    }

    @Override
    public void selectAll() {
        CefLog.Error("TODO: implement selectAll().");
    }
}
