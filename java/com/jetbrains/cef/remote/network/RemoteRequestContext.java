package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.callback.RemoteCompletionCallback;
import com.jetbrains.cef.remote.thrift_codegen.CefValue;
import com.jetbrains.cef.remote.thrift_codegen.CefValueType;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import com.jetbrains.cef.remote.thrift_codegen.RequestContextSettings;
import org.cef.browser.CefRequestContext;
import org.cef.browser.CefRequestContextSettings;
import org.cef.callback.CefCompletionCallback;
import org.cef.handler.CefRequestContextHandler;
import org.cef.misc.CefLog;
import org.cef.misc.Delayed;
import org.cef.network.CefCookieManager;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteRequestContext extends CefRequestContext {
    private final Delayed myDelayed;
    private final CefRequestContextHandler myHandler;
    private final boolean myIsGlobal;
    private RemoteRequestContextHandler myHandlerWrapper;
    private RObject myPeer;
    private RpcContext myRpc;

    // Creates wrapper for global CefRequestContext instance
    public RemoteRequestContext(CefServer server) {
        myRpc = server.getRpcContext();
        myHandler = null;
        myIsGlobal = true;
        myDelayed = new Delayed("RemoteRequestContext(global)");
        myDelayed.finishOnConnection(server, this::createGlobalPeer);
    }

    // Creates wrapper for a new CefRequestContext instance (with default settings)
    public RemoteRequestContext(CefServer server, CefRequestContextHandler handler) {
        this(server, (CefRequestContextSettings) null, handler);
    }

    // Creates wrapper for a new CefRequestContext instance with the specified settings
    public RemoteRequestContext(CefServer server, CefRequestContextSettings settings, CefRequestContextHandler handler) {
        myRpc = server.getRpcContext();
        myHandler = handler;
        myIsGlobal = false;
        myDelayed = new Delayed("RemoteRequestContext(browser)");
        final RequestContextSettings rcs = toThriftSettings(settings);
        myDelayed.finishOnConnection(server, () -> createPeer(rcs));
    }

    // Creates wrapper for a new CefRequestContext instance that shares storage with |other|
    public RemoteRequestContext(CefServer server, CefRequestContext other, CefRequestContextHandler handler) {
        myRpc = server.getRpcContext();
        myHandler = handler;
        myIsGlobal = false;
        myDelayed = new Delayed("RemoteRequestContext(shared)");
        if (!(other instanceof RemoteRequestContext)) {
            CefLog.Error("RemoteRequestContext: can't share storage with context of class %s",
                    other == null ? null : other.getClass().getName());
            myDelayed.finishOnConnection(server);
            return;
        }
        final RemoteRequestContext rother = (RemoteRequestContext) other;
        myDelayed.finishOnConnection(server, () -> createSharedPeer(rother));
    }

    public RObject getPeer() { return myPeer; }

    RpcContext getRpcContext() { return myRpc; }

    void runOrDelay(Runnable action, String name) {
        myDelayed.runOrDelay(action, name);
    }

    private static RequestContextSettings toThriftSettings(CefRequestContextSettings settings) {
        RequestContextSettings rcs = new RequestContextSettings();
        if (settings == null)
            return rcs;
        rcs.setCachePath(settings.cache_path);
        rcs.setPersistSessionCookies(settings.persist_session_cookies);
        rcs.setAcceptLanguageList(settings.accept_language_list);
        rcs.setCookieableSchemesList(settings.cookieable_schemes_list);
        rcs.setCookieableSchemesExcludeDefaults(settings.cookieable_schemes_exclude_defaults);
        return rcs;
    }

    private void createGlobalPeer() {
        myPeer = myRpc.execObj(s -> s.RequestContext_GetGlobal());
    }

    private void createPeer(RequestContextSettings settings) {
        RemoteRequestContextHandler rhandler = getRemoteHandler();
        myPeer = myRpc.execObj(s -> s.RequestContext_Create(rhandler == null ? new RObject() : rhandler.toRObject(), settings));
    }

    private void createSharedPeer(RemoteRequestContext other) {
        RObject otherPeer = other.getPeer();
        if (otherPeer == null || otherPeer.isNull) {
            CefLog.Error("RemoteRequestContext: can't share storage with context %s: its server peer isn't created", other);
            return;
        }
        RemoteRequestContextHandler rhandler = getRemoteHandler();
        myPeer = myRpc.execObj(s -> s.RequestContext_CreateShared(otherPeer, rhandler == null ? new RObject() : rhandler.toRObject()));
    }

    // Returns the server-side peer or null (with error logging) if it isn't available.
    private RObject peerOrNull(String methodName) {
        if (myRpc == null) {
            CefLog.Error("RemoteRequestContext: %s called when rpc-context is null.", methodName);
            return null;
        }
        RObject peer = myPeer;
        if (peer == null || peer.isNull) {
            CefLog.Error("RemoteRequestContext: %s called when server peer isn't created (or was disposed).", methodName);
            return null;
        }
        return peer;
    }

    @Override
    public void dispose() {
        // 1. Dispose handler object
        if (myHandlerWrapper != null)
            RemoteRequestContextHandler.FACTORY.dispose(myHandlerWrapper.getId());

        // 2. Dispose context object
        if (myPeer != null && !myPeer.isNull) {
            myRpc.invokeLater(s -> {
                s.RequestContext_Dispose(myPeer);
                myPeer = null;
            });
        }
    }

    @Override
    public boolean isGlobal() {
        return myIsGlobal;
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
    public boolean isSame(CefRequestContext other) {
        if (other == this)
            return true;
        if (!(other instanceof RemoteRequestContext))
            return false;
        RObject otherPeer = ((RemoteRequestContext) other).getPeer();
        if (otherPeer == null || otherPeer.isNull)
            return false;
        RObject peer = peerOrNull("isSame");
        return peer != null && Boolean.TRUE.equals(myRpc.execObj(s -> s.RequestContext_IsSame(peer, otherPeer)));
    }

    @Override
    public boolean isSharingWith(CefRequestContext other) {
        if (!(other instanceof RemoteRequestContext))
            return false;
        RObject otherPeer = ((RemoteRequestContext) other).getPeer();
        if (otherPeer == null || otherPeer.isNull)
            return false;
        RObject peer = peerOrNull("isSharingWith");
        return peer != null && Boolean.TRUE.equals(myRpc.execObj(s -> s.RequestContext_IsSharingWith(peer, otherPeer)));
    }

    @Override
    public String getCachePath() {
        RObject peer = peerOrNull("getCachePath");
        if (peer == null)
            return "";
        String result = myRpc.execObj(s -> s.RequestContext_GetCachePath(peer));
        return result != null ? result : "";
    }

    @Override
    public CefCookieManager getCookieManager() {
        return RemoteCookieManager.createForContext(this);
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
        myDelayed.runOrDelay(() -> myRpc.invokeLater(s -> s.RequestContext_ClearCertificateExceptions(myPeer, cbId)), "ClearCertificateExceptions");
    }

    @Override
    public void CloseAllConnections(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).toRObject() : new RObject();
        myDelayed.runOrDelay(() -> myRpc.invokeLater(s -> s.RequestContext_CloseAllConnections(myPeer, cbId)), "CloseAllConnections");
    }

    @Override
    public void clearHttpCache(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).toRObject() : new RObject();
        myDelayed.runOrDelay(() -> myRpc.invokeLater(s -> s.RequestContext_ClearHttpCache(myPeer, cbId)), "clearHttpCache");
    }

    @Override
    public void clearHttpAuthCredentials(CefCompletionCallback callback) {
        RObject cbId = callback != null ? RemoteCompletionCallback.create(callback).toRObject() : new RObject();
        myDelayed.runOrDelay(() -> myRpc.invokeLater(s -> s.RequestContext_ClearHttpAuthCredentials(myPeer, cbId)), "clearHttpAuthCredentials");
    }
}
