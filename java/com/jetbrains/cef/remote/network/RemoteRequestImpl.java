package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObjectLocal;
import com.jetbrains.cef.remote.thrift_codegen.PostData;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.misc.CefLog;
import org.cef.network.CefPostData;
import org.cef.network.CefRequest;

import java.util.Map;

// 1. Represents remote java peer for native server object (CefRequest).
// 2. Created on java side when processing some server request.
// 3. Lifetime of remote native peer if managed by server: native object
// peer (CefRequest) is destroyed immediately after rpc finished. After that
// moment all requests from java to native will return errors (or default values).
// Java object will be destroyed via usual gc.
public class RemoteRequestImpl extends RemoteServerObjectLocal {
    public RemoteRequestImpl(RpcExecutor server, RObject request) {
        super(server, request);
    }

    @Override
    public void flush() {
        myServer.exec((s)->{
            s.Request_Update(thriftIdWithCache());
        });
    }

    public long getIdentifier() { return getLongVal("Identifier"); }

    public boolean isReadOnly() { return getBoolVal("IsReadOnly"); }

    public String getURL() { return myCache.get("URL"); }

    public void setURL(String url) { setStrVal("URL", url); }

    public String getMethod() { return myCache.get("Method"); }

    public void setMethod(String method) { setStrVal("Method", method); }

    public void setReferrer(String url, CefRequest.ReferrerPolicy policy) {
        setStrVal("ReferrerURL", url);
        setStrVal("ReferrerPolicy", policy.name());
    }

    public String getReferrerURL() { return myCache.get("ReferrerURL"); }

    public CefRequest.ReferrerPolicy getReferrerPolicy() {
        String sval = myCache.get("ReferrerPolicy");
        if (sval != null && !sval.isEmpty()) {
            try {
                return CefRequest.ReferrerPolicy.valueOf(sval);
            } catch (IllegalArgumentException e) {
                CefLog.Error("getReferrerPolicy: ", e.getMessage());
            }
        }
        return null;// probably REFERRER_POLICY_DEFAULT ?
    }

    public CefPostData getPostData() {
        PostData pd = myServer.execObj((s)-> s.Request_GetPostData(thriftId()));
        return pd == null ? null : new RemotePostData(pd);
    }

    public void setPostData(CefPostData postData) {
        myServer.exec((s)->{
            s.Request_SetPostData(thriftId(), RemotePostData.toThriftWithMap(postData));
        });
    }

    public void set(String url, String method, CefPostData postData, Map<String, String> headerMap) {
        myServer.exec((s)->{
            s.Request_Set(thriftId(), url, method, RemotePostData.toThriftWithMap(postData), headerMap);
        });
    }

    public String getHeaderByName(String name) {
        return myServer.execObj((s)->s.Request_GetHeaderByName(thriftId(), name));
    }

    public void setHeaderByName(String name, String value, boolean overwrite) {
        myServer.exec((s)->{
            s.Request_SetHeaderByName(thriftId(), name, value, overwrite);
        });
    }

    public void getHeaderMap(Map<String, String> headerMap) {
        if (headerMap == null)
            return;
        Map<String, String> result = myServer.execObj((s)-> s.Request_GetHeaderMap(thriftId()));
        if (result != null)
            headerMap.putAll(result);
    }

    public void setHeaderMap(Map<String, String> headerMap) {
        myServer.exec((s)->{
            s.Request_SetHeaderMap(thriftId(), headerMap);
        });
    }

    public int getFlags() { return (int)getLongVal("Flags"); }

    public void setFlags(int flags) { setLongVal("Flags", flags); }

    public String getFirstPartyForCookies() { return myCache.get("FirstPartyForCookies"); }

    public void setFirstPartyForCookies(String url) { setStrVal("FirstPartyForCookies", url); }

    public CefRequest.ResourceType getResourceType() {
        String sval = myCache.get("ResourceType");
        if (sval != null && !sval.isEmpty()) {
            try {
                return CefRequest.ResourceType.valueOf(sval);
            } catch (IllegalArgumentException e) {
                CefLog.Error("getResourceType: ", e.getMessage());
            }
        }
        return null;
    }

    public CefRequest.TransitionType getTransitionType() {
        String sval = myCache.get("TransitionType");
        if (sval != null && !sval.isEmpty()) {
            try {
                return CefRequest.TransitionType.valueOf(sval);
            } catch (IllegalArgumentException e) {
                CefLog.Error("getTransitionType: ", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public String toString() {
        // TODO: use return CefRequest.toString(this) after debugging
        return myCache.toString();
    }
}
