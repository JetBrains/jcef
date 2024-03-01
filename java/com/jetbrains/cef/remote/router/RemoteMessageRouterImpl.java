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
public class RemoteMessageRouterImpl extends RemoteServerObject {
    private final List<RemoteMessageRouterHandler> myHandlers = new ArrayList<>(); // used to manage lifetime of handlers
    private String myQuery;
    private String myCancel;

    private RemoteMessageRouterImpl(RpcExecutor server, RObject robj, String query, String cancel) {
        super(server, robj);
        myQuery = query;
        myCancel = cancel;
    }

    public static RemoteMessageRouterImpl create(RpcExecutor server, CefMessageRouter.CefMessageRouterConfig config) {
        RObject robj = server.execObj((s)->s.MessageRouter_Create(config.jsQueryFunction, config.jsCancelFunction));
        if (robj.objId < 0) {
            CefLog.Error("MessageRouter_Create returns invalid objId %d.", robj.objId);
            return null;
        }
        return new RemoteMessageRouterImpl(server, robj, config.jsQueryFunction, config.jsCancelFunction);
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
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.create(handler);
        //CefLog.Debug("%s add handler %s [%d]", this, rhandler, rhandler.getId());
        synchronized (myHandlers) {
            myHandlers.add(rhandler);
        }
        myServer.exec((s)->s.MessageRouter_AddHandler(thriftId(), rhandler.thriftId(), first));
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
        myServer.exec((s)->s.MessageRouter_RemoveHandler(thriftId(), rhandler.thriftId()));
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
            myServer.exec((s) -> s.MessageRouter_CancelPending(thriftId(), bid, rhandler.thriftId()));
        }
    }

    @Override
    public String toString() {
        return String.format("Router<%s | %s>", myQuery, myCancel);
    }
}
