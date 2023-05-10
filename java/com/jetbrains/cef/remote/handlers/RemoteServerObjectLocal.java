package com.jetbrains.cef.remote.handlers;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.apache.thrift.TException;
import org.cef.misc.CefLog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

//
// Represents server object that valid only in current method context (destroyed on server after rpc executed)
//
public abstract class RemoteServerObjectLocal {
    protected final int myId;
    protected final RpcExecutor myServer;
    protected final Map<String, String> myCache = new HashMap<>();

    public RemoteServerObjectLocal(RpcExecutor server, RObject robj) {
        myId = robj.objId;
        myServer = server;
        if (robj.objInfo != null)
            myCache.putAll(robj.objInfo);
    }

    public abstract void flush();

    //
    // Protected API
    //

    protected RObject thriftId() {
        return new RObject(myId);
    }
    protected RObject thriftIdWithCache() {
        return new RObject(myId).setObjInfo(myCache);
    }

    protected void onThriftException(TException e) {
        CefLog.Error("thrift exception '%s'", e.getMessage());
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        CefLog.Error(sw.getBuffer().toString());
    }

    protected long getLongVal(String key) {
        return getLongVal(key, 0);
    }
    protected long getLongVal(String key, long def) {
        String sval = myCache.get(key);
        if (sval == null || sval.isEmpty())
            return def;
        try {
            return Long.parseLong(sval);
        } catch (NumberFormatException e) {
            CefLog.Error("not long value: %s -> %s", key, sval);
        }
        return def;
    }

    protected boolean getBoolVal(String key) {
        return getBoolVal(key, false);
    }
    protected boolean getBoolVal(String key, boolean def) {
        String sval = myCache.get(key);
        if (sval == null || sval.isEmpty())
            return def;
        try {
            return Boolean.parseBoolean(sval);
        } catch (NumberFormatException e) {
            CefLog.Error("not bool value: %s -> %s", key, sval);
        }
        return def;
    }

    protected void setStrVal(String key, String value) {
        if (value == null || value.isEmpty())
            myCache.remove(key);
        else
            myCache.put(key, value);
    }
    protected void setLongVal(String key, long value) {
        myCache.put(key, String.valueOf(value));
    }
}
