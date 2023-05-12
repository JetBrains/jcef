package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObjectLocal;
import com.jetbrains.cef.remote.thrift_codegen.PostData;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.misc.CefLog;
import org.cef.network.CefPostData;
import org.cef.network.CefRequest;

import java.util.Map;

public class RemoteRequest extends RemoteServerObjectLocal implements CefRequest {
    public RemoteRequest(RpcExecutor server, RObject request) {
        super(server, request);
    }

    public void flush() {
        myServer.exec((s)->{
            s.Request_Update(thriftIdWithCache());
        });
    }

    @Override
    public long getIdentifier() { return getLongVal("Identifier"); }

    @Override
    public boolean isReadOnly() { return getBoolVal("IsReadOnly"); }

    @Override
    public String getURL() { return myCache.get("URL"); }

    @Override
    public void setURL(String url) { setStrVal("URL", url); }

    @Override
    public String getMethod() { return myCache.get("Method"); }

    @Override
    public void setMethod(String method) { setStrVal("Method", method); }

    @Override
    public void setReferrer(String url, ReferrerPolicy policy) {
        setStrVal("ReferrerURL", url);
        setStrVal("ReferrerPolicy", policy.name());
    }

    @Override
    public String getReferrerURL() { return myCache.get("ReferrerURL"); }

    @Override
    public ReferrerPolicy getReferrerPolicy() {
        String sval = myCache.get("ReferrerPolicy");
        if (sval != null && !sval.isEmpty()) {
            try {
                return ReferrerPolicy.valueOf(sval);
            } catch (IllegalArgumentException e) {
                CefLog.Error("getReferrerPolicy: ", e.getMessage());
            }
        }
        return null;// probably REFERRER_POLICY_DEFAULT ?
    }

    @Override
    public CefPostData getPostData() {
        PostData pd = myServer.execObj((s)-> s.Request_GetPostData(thriftId()));
        return pd == null ? null : new RemotePostData(pd);
    }

    @Override
    public void setPostData(CefPostData postData) {
        myServer.exec((s)->{
            s.Request_SetPostData(thriftId(), RemotePostData.toThriftWithMap(postData));
        });
    }

    @Override
    public void set(String url, String method, CefPostData postData, Map<String, String> headerMap) {
        myServer.exec((s)->{
            s.Request_Set(thriftId(), url, method, RemotePostData.toThriftWithMap(postData), headerMap);
        });
    }

    @Override
    public String getHeaderByName(String name) {
        return myServer.execObj((s)->s.Request_GetHeaderByName(thriftId(), name));
    }

    @Override
    public void setHeaderByName(String name, String value, boolean overwrite) {
        myServer.exec((s)->{
            s.Request_SetHeaderByName(thriftId(), name, value, overwrite);
        });
    }

    @Override
    public void getHeaderMap(Map<String, String> headerMap) {
        if (headerMap == null)
            return;
        Map<String, String> result = myServer.execObj((s)-> s.Request_GetHeaderMap(thriftId()));
        if (result != null)
            headerMap.putAll(result);
    }

    @Override
    public void setHeaderMap(Map<String, String> headerMap) {
        myServer.exec((s)->{
            s.Request_SetHeaderMap(thriftId(), headerMap);
        });
    }

    @Override
    public int getFlags() { return (int)getLongVal("Flags"); }

    @Override
    public void setFlags(int flags) { setLongVal("Flags", flags); }

    @Override
    public String getFirstPartyForCookies() { return myCache.get("FirstPartyForCookies"); }

    @Override
    public void setFirstPartyForCookies(String url) { setStrVal("FirstPartyForCookies", url); }

    @Override
    public ResourceType getResourceType() {
        String sval = myCache.get("ResourceType");
        if (sval != null && !sval.isEmpty()) {
            try {
                return ResourceType.valueOf(sval);
            } catch (IllegalArgumentException e) {
                CefLog.Error("getResourceType: ", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public TransitionType getTransitionType() {
        String sval = myCache.get("TransitionType");
        if (sval != null && !sval.isEmpty()) {
            try {
                return TransitionType.valueOf(sval);
            } catch (IllegalArgumentException e) {
                CefLog.Error("getTransitionType: ", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public String toString() {
        // TODO: use return CefRequestBase.toString(this) after debugging
        return myCache.toString();
    }
}
