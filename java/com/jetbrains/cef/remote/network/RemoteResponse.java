package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObjectLocal;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.handler.CefLoadHandler;
import org.cef.misc.CefLog;
import org.cef.network.CefResponse;

import java.util.Map;

// 1. Represents remote java peer for native server object.
// 2. Created on java side when processing some server request.
// 3. Lifetime of remote native peer if managed by server: native object
// peer (CefRequest) is destroyed immediately after rpc finished. After that
// moment all requests from java to native will return errors (or default values).
// Java object will be destroyed via usual gc.
public class RemoteResponse extends RemoteServerObjectLocal implements CefResponse {
    public RemoteResponse(RpcExecutor server, RObject resp) {
        super(server, resp);
    }

    public void flush() {
        myServer.exec((s)->{
            s.Response_Update(thriftIdWithCache());
        });
    }

    @Override
    public boolean isReadOnly() { return getBoolVal("IsReadOnly"); }

    @Override
    public CefLoadHandler.ErrorCode getError() {
        String sval = myCache.get("Error");
        if (sval != null && !sval.isEmpty()) {
            try {
                return CefLoadHandler.ErrorCode.valueOf(sval);
            } catch (IllegalArgumentException e) {
                CefLog.Error("getError: ", e.getMessage());
            }
        }
        return CefLoadHandler.ErrorCode.ERR_NONE;
    }

    @Override
    public void setError(CefLoadHandler.ErrorCode errorCode) {
        setLongVal("Error", errorCode.getCode());
    }

    @Override
    public int getStatus() { return (int)getLongVal("Status"); }

    @Override
    public void setStatus(int status) { setLongVal("Status", status); }

    @Override
    public String getStatusText() { return myCache.get("StatusText"); }

    @Override
    public void setStatusText(String statusText) { setStrVal("StatusText", statusText); }

    @Override
    public String getMimeType() { return myCache.get("MimeType"); }

    @Override
    public void setMimeType(String mimeType) { setStrVal("MimeType", mimeType); }

    @Override
    public String getHeaderByName(String name) {
        return myServer.execObj((s)-> s.Response_GetHeaderByName(thriftId(), name));
    }

    @Override
    public void setHeaderByName(String name, String value, boolean overwrite) {
        myServer.exec((s)-> s.Response_SetHeaderByName(thriftId(), name, value, overwrite));
    }

    @Override
    public void getHeaderMap(Map<String, String> headerMap) {
        if (headerMap == null)
            return;
        Map<String, String> result = myServer.execObj((s)-> s.Response_GetHeaderMap(thriftId()));
        if (result != null)
            headerMap.putAll(result);
    }

    @Override
    public void setHeaderMap(Map<String, String> headerMap) {
        myServer.exec((s)-> s.Response_SetHeaderMap(thriftId(), headerMap));
    }

    @Override
    public String toString() {
        // TODO: use return CefResponse.toString(this) after debugging
        return myCache.toString();
    }
}
