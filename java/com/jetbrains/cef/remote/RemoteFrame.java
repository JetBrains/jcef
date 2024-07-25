package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.CefClient;
import org.cef.browser.CefFrame;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;

// 1. Represents remote java peer for native server object (CefFrame).
// 2. Created
//      a) on java side when processing some server request (and object will be invalid after rpc finished)
//      b) on native side when called CefBrowser::GetMainFrame (object will be valid until GC)
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc.
public class RemoteFrame extends RemoteServerObject implements CefFrame {
    public RemoteFrame(RpcExecutor server, RObject frame) {
        super(server, frame);
    }

    @Override
    protected void disposeOnServerImpl() {
        myServer.exec((s)->{
            s.Frame_Dispose(myId);
        });
    }

    @Override
    public void flush() {
        // Nothing to do (CefFrame is read-only object).
    }

    @Override
    public void dispose() {
        disposeOnServer();
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
        RObject parent = myServer.execObj((s)-> s.Frame_GetParent(myId));
        return parent == null || parent.objId < 0 ? null : new RemoteFrame(myServer, parent);
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        myServer.exec((s)->{
            s.Frame_ExecuteJavaScript(myId, code, url, line);
        });
    }

    @Override
    public void undo() { myServer.exec((s)-> s.Frame_Undo(myId)); }

    @Override
    public void redo() {
        myServer.exec((s)-> s.Frame_Redo(myId));
    }

    @Override
    public void cut() {
        myServer.exec((s)-> s.Frame_Cut(myId));
    }

    @Override
    public void copy() {
        myServer.exec((s)-> s.Frame_Copy(myId));
    }

    @Override
    public void paste() {
        myServer.exec((s)-> s.Frame_Paste(myId));
    }

    @Override
    public void delete() {
        myServer.exec((s)-> s.Frame_Delete(myId));
    }

    @Override
    public void selectAll() {
        myServer.exec((s)-> s.Frame_SelectAll(myId));
    }
}
