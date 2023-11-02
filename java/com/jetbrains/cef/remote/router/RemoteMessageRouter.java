package com.jetbrains.cef.remote.router;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RemoteBrowser;
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
public class RemoteMessageRouter extends RemoteServerObject implements CefMessageRouter {
    private final List<RemoteMessageRouterHandler> myHandlers = new ArrayList<>(); // used to manage lifetime of handlers
    private String myQuery;
    private String myCancel;

    private RemoteMessageRouter(RpcExecutor server, RObject robj, String query, String cancel) {
        super(server, robj);
        myQuery = query;
        myCancel = cancel;
    }

    public static RemoteMessageRouter create(RpcExecutor server) {
        return create(server, "cefQuery", "cefQueryCancel");
    }

    public static RemoteMessageRouter create(String query, String cancel) {
        if (CefServer.instance() == null || CefServer.instance().getService() == null) {
            CefLog.Error("Can't create remote router <%s,%s> because CefServer wasn't initialized.");
            return null;
        }
        return create(CefServer.instance().getService(),query, cancel);
    }

    public static RemoteMessageRouter create(RpcExecutor server, String query, String cancel) {
        // NOTE: impl as in CefMessageRouter_1N_N_1Initialize
        RObject robj = server.execObj((s)->s.MessageRouter_Create(query, cancel));
        if (robj.objId < 0)
            return null;
        return new RemoteMessageRouter(server, robj, query, cancel);
    }

    public void addToBrowser(int bid) {
        myServer.exec((s)->s.MessageRouter_AddMessageRouterToBrowser(thriftId(), bid));
    }

    public void removeFromBrowser(int bid) {
        myServer.exec((s)->s.MessageRouter_RemoveMessageRouterFromBrowser(thriftId(), bid));
    }

    @Override
    public void flush() {}

    @Override
    public void dispose() {
        disposeOnServer();
    }

    @Override
    protected void disposeOnServerImpl() {
        synchronized (myHandlers) {
            for (RemoteMessageRouterHandler h : myHandlers)
                RemoteMessageRouterHandler.FACTORY.dispose(h.getId());
            myHandlers.clear();
        }
        myServer.exec((s)->s.MessageRouter_Dispose(thriftId()));
    }

    // Creates remote wrapper of java handler and stores ref in map.
    // Disposes handler ref in removeHandler (or when router finalizes, see disposeOnServerImpl)
    @Override
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.create(handler);
        CefLog.Debug("%s add handler %s [%d]", this, rhandler, rhandler.getId());
        synchronized (myHandlers) {
            myHandlers.add(rhandler);
        }
        myServer.exec((s)->s.MessageRouter_AddHandler(thriftId(), rhandler.thriftId(true), first));
        return true;
    }

    @Override
    public boolean removeHandler(CefMessageRouterHandler handler) {
        CefLog.Debug("%s remove handler by delegate %s", this, handler);
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.findByDelegate(handler);
        if (rhandler == null)
            return false;

        synchronized (myHandlers) {
            boolean removed = myHandlers.remove(rhandler);
            if (!removed) CefLog.Error("RemoteMessageRouterHandler %s wasn't found in myHandlers list");
        }
        myServer.exec((s)->s.MessageRouter_RemoveHandler(thriftId(), rhandler.thriftId(true)));
        RemoteMessageRouterHandler.FACTORY.dispose(rhandler.getId());
        return true;
    }

    @Override
    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.findByDelegate(handler);
        if (rhandler == null)
            return;

        if (browser != null && !(browser instanceof RemoteBrowser))
            CefLog.Error("Can't cancelPending on non-remote browser " + browser);
        else {
            int bid = browser == null ? -1 : ((RemoteBrowser)browser).getBid();
            myServer.exec((s) -> s.MessageRouter_CancelPending(thriftId(), bid, rhandler.thriftId(true)));
        }
    }

    @Override
    public String toString() {
        return String.format("Router<%s | %s>", myQuery, myCancel);
    }
}
