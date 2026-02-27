package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.callback.RemoteCompletionCallback;
import com.jetbrains.cef.remote.thrift_codegen.CefValue;
import com.jetbrains.cef.remote.thrift_codegen.CefValueType;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefCompletionCallback;
import org.cef.handler.CefRequestContextHandler;
import org.cef.misc.CefLog;
import org.cef.misc.Delayed;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteRequestContext extends CefRequestContext {
    private final Delayed myDelayed;
    private final CefRequestContextHandler myHandler;
    private RemoteRequestContextHandler myHandlerWrapper;
    private RObject myPeer;
    private RpcContext myRpc;

    // Creates wrapper for global CefRequestContext instance
    public RemoteRequestContext(CefServer server) {
        myRpc = server.getRpcContext();
        myHandler = null;
        myDelayed = new Delayed("RemoteRequestContext(global)");
        myDelayed.finishOnConnection(server, this::createGlobalPeer);
    }

    // Creates wrapper for browser's CefRequestContext instance (can be 'global' too)
    public RemoteRequestContext(CefServer server, CefRequestContextHandler handler) {
        myRpc = server.getRpcContext();
        myHandler = handler;
        myDelayed = new Delayed("RemoteRequestContext(browser)");
        myDelayed.finishOnConnection(server, this::createPeer);
    }

    public RObject getPeer() { return myPeer; }

    private void createGlobalPeer() {
        myPeer = myRpc.execObj(s -> s.RequestContext_GetGlobal());
    }

    private void createPeer() {
        RemoteRequestContextHandler rhandler = getRemoteHandler();
        myPeer = myRpc.execObj(s -> s.RequestContext_Create(rhandler == null ? new RObject() : rhandler.toRObject()));
    }

    @Override
    public void dispose() {
        // 1. Dispose handler object
        if (myHandlerWrapper != null)
            RemoteRequestContextHandler.FACTORY.dispose(myHandlerWrapper.getId());

        // 2. Dispose context object
        if (myPeer != null && !myPeer.isNull) {
            myRpc.exec(s -> s.RequestContext_Dispose(myPeer));
            myPeer = null;
        }
    }

    @Override
    public boolean isGlobal() {
        return myHandler == null;
    }

    @Override
    public CefRequestContextHandler getHandler() {
        return myHandler;
    }

    private RemoteRequestContextHandler getRemoteHandler() {
        if (myHandler == null)
            return null;
        if (myHandlerWrapper == null)
            myHandlerWrapper = RemoteRequestContextHandler.create(myHandler);
        return myHandlerWrapper;
    }

    @Override
    public boolean hasPreference(String name) {
        if (myRpc == null) {
            CefLog.Error("RemoteRequestContext: hasPreference(%s) called when rpc-context is null.", name);
            return false;
        }
        return myRpc.execObj(s -> s.RequestContext_HasPreference(myPeer, name));
    }

    private static Object fromCefValue(CefValue cv) {
        if (cv == null || cv.getType() == CefValueType.NONE)
            return null;

        switch (cv.getType()) {
            case BOOL: return Boolean.valueOf(cv.intVal != 0);
            case INT: return Long.valueOf(cv.intVal);
            case DOUBLE: return Double.valueOf(cv.doubleVal);
            case STRING: return cv.strVal;
            case BYTE_BUFFER: return cv.binVal;
            case MAP: return cv.mapVal;
            case LIST: return cv.listVal;
            default: {
                CefLog.Error("RemoteRequestContext::fromCefValue: unknown type: %s", cv.getType());
                return null;
            }
        }
    }

    private static CefValue toCefValue(Object obj) {
        if (obj == null)
            return null;

        CefValue result = null;
        if (obj instanceof Boolean) {
            boolean val = (Boolean)obj;
            result = new CefValue(CefValueType.BOOL);
            result.setIntVal(val ? 1 : 0);
        } else if (obj instanceof Integer) {
            int val = (Integer)obj;
            result = new CefValue(CefValueType.INT);
            result.setIntVal(val);
        } else if (obj instanceof Long) {
            long val = (Long)obj;
            result = new CefValue(CefValueType.INT);
            result.setIntVal(val);
        } else if (obj instanceof Double) {
            double val = (Double)obj;
            result = new CefValue(CefValueType.DOUBLE);
            result.setDoubleVal(val);
        } else if (obj instanceof String) {
            String val = (String)obj;
            result = new CefValue(CefValueType.STRING);
            result.setStrVal(val);
        } else if (obj instanceof ByteBuffer) {
            ByteBuffer val = (ByteBuffer)obj;
            result = new CefValue(CefValueType.BYTE_BUFFER);
            result.setBinVal(val);
        } else if (obj instanceof Map) {
            Map val = (Map)obj;
            result = new CefValue(CefValueType.MAP);
            result.setMapVal(val);
        } else if (obj instanceof List) {
            List val = (List)obj;
            result = new CefValue(CefValueType.LIST);
            result.setListVal(val);
        } else {
            CefLog.Error("RemoteRequestContext::toCefValue: unknown object type: %s [class=%s]", obj, obj.getClass().getName());
        }
        return result;
    }

    @Override
    public Object getPreference(String name) {
        if (myRpc == null) {
            CefLog.Error("RemoteRequestContext: getPreference(%s) called when rpc-context is null.", name);
            return false;
        }
        CefValue result = myRpc.execObj(s -> s.RequestContext_GetPreference(myPeer, name));
        return fromCefValue(result);
    }

    @Override
    public Map<String, Object> getAllPreferences(boolean includeDefaults) {
        if (myRpc == null) {
            CefLog.Error("RemoteRequestContext: getAllPreferences called when rpc-context is null.");
            return null;
        }
        Map<String, CefValue> result = myRpc.execObj(s -> s.RequestContext_GetAllPreferences(myPeer, includeDefaults));
        if (result == null || result.isEmpty())
            return null;

        Map<String, Object> retVal = new HashMap<>();
        result.forEach((k, v) -> retVal.put(k, fromCefValue(v)));
        return retVal;
    }

    @Override
    public boolean canSetPreference(String name) {
        if (myRpc == null) {
            CefLog.Error("RemoteRequestContext: canSetPreference(%s) called when rpc-context is null.", name);
            return false;
        }
        return myRpc.execObj(s -> s.RequestContext_CanSetPreference(myPeer, name));
    }

    @Override
    public String setPreference(String name, Object value) {
        if (myRpc == null) {
            CefLog.Error("RemoteRequestContext: setPreference(%s) called when rpc-context is null.", name);
            return null;
        }

        CefValue cv = toCefValue(value);
        if (cv == null) {
            CefLog.Error("RemoteRequestContext: setPreference(%s) called and toCefValue returns null for object %s", name, value);
            return null;
        }

        return myRpc.execObj(s -> s.RequestContext_SetPreference(myPeer, name, cv));
    }

    @Override
    public void ClearCertificateExceptions(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).toRObject() : new RObject();
        myDelayed.runOrSchedule(() -> myRpc.invokeLater(s -> s.RequestContext_ClearCertificateExceptions(myPeer, cbId)), "ClearCertificateExceptions");
    }

    @Override
    public void CloseAllConnections(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).toRObject() : new RObject();
        myDelayed.runOrSchedule(() -> myRpc.invokeLater(s -> s.RequestContext_CloseAllConnections(myPeer, cbId)), "CloseAllConnections");
    }
}
