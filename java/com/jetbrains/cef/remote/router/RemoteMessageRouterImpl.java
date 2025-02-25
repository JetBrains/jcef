package com.jetbrains.cef.remote.router;

import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.browser.RemoteBrowser;
import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.misc.CefLog;

import java.util.ArrayList;
import java.util.List;

// 1. Represent remote java peer for native server object (CefMessageRouter) that
// valid in any context (destroyed on server manually, via rpc from java side).
// 2. Created on java side when user configures RemoteMessageRouter (object is stored in RemoteClient's internal map).
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc.
public class RemoteMessageRouterImpl extends RemoteServerObject {
    private final List<RemoteMessageRouterHandler> myHandlers = new ArrayList<>(); // used to manage lifetime of handlers
    private String myQuery;
    private String myCancel;

    private RemoteMessageRouterImpl(RpcContext rpcContext, RObject robj, String query, String cancel) {
        super(rpcContext, robj);
        myQuery = query;
        myCancel = cancel;
    }

    public static RemoteMessageRouterImpl create(RpcContext rpcContext, CefMessageRouter.CefMessageRouterConfig config) {
        if (config == null)
            config = new CefMessageRouter.CefMessageRouterConfig(); // Use default config (the same logic as in CefMessageRouter_N).
        final String jsQueryFunction = config.jsQueryFunction;
        final String jsCancelFunction = config.jsCancelFunction;
        RObject robj = rpcContext.main.execObj((s)->s.MessageRouter_Create(jsQueryFunction, jsCancelFunction));
        if (robj.objId < 0) {
            CefLog.Error("MessageRouter_Create returns invalid objId %d (queryFunction='%s', cancelFunction='%s')", robj.objId, jsQueryFunction, jsCancelFunction);
            return null;
        }
        return new RemoteMessageRouterImpl(rpcContext, robj, jsQueryFunction, jsCancelFunction);
    }

    public void addToBrowser(int bid) {
        myRpc.main.exec((s)->s.MessageRouter_AddMessageRouterToBrowser(thriftId(), bid));
    }

    public void removeFromBrowser(int bid) {
        myRpc.main.exec((s)->s.MessageRouter_RemoveMessageRouterFromBrowser(thriftId(), bid));
    }

    @Override
    public void flush() {}

    @Override
    protected void disposeOnServerImpl() {
        synchronized (myHandlers) {
            for (RemoteMessageRouterHandler h : myHandlers)
                RemoteMessageRouterHandler.FACTORY.dispose(h.getId());
            myHandlers.clear();
        }
        final RObject id = thriftId();
        myRpc.invokeLater(s -> s.MessageRouter_Dispose(id));
    }

    // Creates remote wrapper of java handler and stores ref in map.
    // Disposes handler ref in removeHandler (or when router finalizes, see disposeOnServerImpl)
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.create(handler);
        //CefLog.Debug("%s add handler %s [%d]", this, rhandler, rhandler.getId());
        synchronized (myHandlers) {
            myHandlers.add(rhandler);
        }
        myRpc.main.exec((s)->s.MessageRouter_AddHandler(thriftId(), rhandler.thriftId(), first));
        return true;
    }

    public boolean removeHandler(CefMessageRouterHandler handler) {
        CefLog.Debug("%s remove handler by delegate %s", this, handler);
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.findByDelegate(handler);
        if (rhandler == null)
            return false;

        synchronized (myHandlers) {
            boolean removed = myHandlers.remove(rhandler);
            if (!removed) CefLog.Error("RemoteMessageRouterHandler %s wasn't found in myHandlers list");
        }
        myRpc.main.exec((s)->s.MessageRouter_RemoveHandler(thriftId(), rhandler.thriftId()));
        RemoteMessageRouterHandler.FACTORY.dispose(rhandler.getId());
        return true;
    }

    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.findByDelegate(handler);
        if (rhandler == null)
            return;

        if (browser != null && !(browser instanceof RemoteBrowser))
            CefLog.Error("Can't cancelPending on non-remote browser " + browser);
        else {
            int bid = browser == null ? -1 : ((RemoteBrowser)browser).getBid();
            myRpc.main.exec((s) -> s.MessageRouter_CancelPending(thriftId(), bid, rhandler.thriftId()));
        }
    }

    @Override
    public String toString() {
        return String.format("Router<%s | %s>", myQuery, myCancel);
    }
}
