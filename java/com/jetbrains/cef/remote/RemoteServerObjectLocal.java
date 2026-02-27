package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.RObject;
import com.jetbrains.cef.remote.thrift.TException;
import org.cef.misc.CefLog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

// 1. Direct inheritors represent remote java peer for native server object that
// valid only in current method context.
// 2. Created on java side when processing some server request.
// 3. Lifetime of remote native peer if managed by server and native object
// peer is destroyed immediately after rpc finished. After that
// moment all requests from java to native will return errors (or default values).
// Java object peer will be destroyed via usual gc.
public abstract class RemoteServerObjectLocal {
    protected final int myId;
    protected final RpcContext myRpc;
    protected final Map<String, String> myCache = new HashMap<>();

    public RemoteServerObjectLocal(RpcContext rpcContext, RObject robj) {
        myId = robj.uid;
        myRpc = rpcContext;
        if (robj.info != null)
            myCache.putAll(robj.info);
    }

    public abstract void flush();

    public RObject toRObject() { return new RObject(false, myId); }
    public RObject toRObjectWithCache() { return new RObject(false, myId).setInfo(myCache); }

    //
    // Protected API
    //

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
