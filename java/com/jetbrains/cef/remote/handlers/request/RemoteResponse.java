package com.jetbrains.cef.remote.handlers.request;

import com.jetbrains.cef.remote.handlers.RemoteServerObjectLocal;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import com.jetbrains.cef.remote.thrift_codegen.Server;
import org.apache.thrift.TException;
import org.cef.handler.CefLoadHandler;
import org.cef.misc.CefLog;
import org.cef.network.CefResponse;
import org.cef.network.CefResponseBase;

import java.util.Map;

public class RemoteResponse extends RemoteServerObjectLocal implements CefResponse {
    public RemoteResponse(Server.Client server, RObject resp) {
        super(server, resp);
    }

    public void flush() {
        try {
            myServer.Response_Update(thriftIdWithCache());
        } catch (TException e) {
            onThriftException(e);
        }
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
        try {
            return myServer.Response_GetHeaderByName(thriftId(), name);
        } catch (TException e) {
            onThriftException(e);
        }
        return null;
    }

    @Override
    public void setHeaderByName(String name, String value, boolean overwrite) {
        try {
            myServer.Response_SetHeaderByName(thriftId(), name, value, overwrite);
        } catch (TException e) {
            onThriftException(e);
        }
    }

    @Override
    public void getHeaderMap(Map<String, String> headerMap) {
        if (headerMap == null)
            return;
        try {
            Map<String, String> result = myServer.Response_GetHeaderMap(thriftId());
            if (result != null)
                headerMap.putAll(result);
        } catch (TException e) {
            onThriftException(e);
        }
    }

    @Override
    public void setHeaderMap(Map<String, String> headerMap) {
        try {
            myServer.Response_SetHeaderMap(thriftId(), headerMap);
        } catch (TException e) {
            onThriftException(e);
        }
    }

    @Override
    public String toString() {
        // TODO: use return CefResponseBase.toString(this) after debugging
        return myCache.toString();
    }
}
