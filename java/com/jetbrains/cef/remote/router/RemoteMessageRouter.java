package com.jetbrains.cef.remote.router;

import com.jetbrains.cef.remote.RemoteBrowser;
import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.misc.CefLog;

public class RemoteMessageRouter extends RemoteServerObject implements CefMessageRouter {
    private RemoteMessageRouter(RpcExecutor server, RObject robj) {
        super(server, robj);
    }

    public static RemoteMessageRouter create(RpcExecutor server) {
        return create(server, "cefQuery", "cefQueryCancel");
    }

    public static RemoteMessageRouter create(RpcExecutor server, String query, String cancel) {
        // NOTE: impl as in CefMessageRouter_1N_N_1Initialize
        RObject robj = server.execObj((s)->s.CreateMessageRouter(query, cancel));
        if (robj.objId < 0)
            return null;
        return new RemoteMessageRouter(server, robj);
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
        myServer.exec((s)->s.MessageRouter_Dispose(thriftId()));
    }

    @Override
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        // TODO: support first flag
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.find(handler, true);
        myServer.exec((s)->s.MessageRouter_AddHandler(thriftId(), rhandler.thriftId(true)));
        return true;
    }

    @Override
    public boolean removeHandler(CefMessageRouterHandler handler) {
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.find(handler, false);
        if (rhandler == null)
            return false;

        myServer.exec((s)->s.MessageRouter_RemoveHandler(thriftId(), rhandler.thriftId(true)));
        return true;
    }

    @Override
    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {
        RemoteMessageRouterHandler rhandler = RemoteMessageRouterHandler.find(handler, false);
        if (rhandler == null)
            return;

        if (browser != null && !(browser instanceof RemoteBrowser))
            CefLog.Error("Can't cancelPending on non-remote browser " + browser);
        else {
            int bid = browser == null ? -1 : ((RemoteBrowser)browser).getBid();
            myServer.exec((s) -> s.MessageRouter_CancelPending(thriftId(), bid, rhandler.thriftId(true)));
        }
    }
}
