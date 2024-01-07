package com.jetbrains.cef.remote.network;

import org.cef.handler.CefLoadHandler;
import org.cef.network.CefResponse;

import java.util.Map;

public class RemoteResponse extends CefResponse {
    private final RemoteResponseImpl myImpl;

    public RemoteResponse(RemoteResponseImpl impl) {
        super();
        this.myImpl = impl;
    }

    @Override
    public void dispose() {}

    @Override
    public boolean isReadOnly() {
        return myImpl.isReadOnly();
    }

    @Override
    public CefLoadHandler.ErrorCode getError() {
        return myImpl.getError();
    }

    @Override
    public void setError(CefLoadHandler.ErrorCode errorCode) {
        myImpl.setError(errorCode);
    }

    @Override
    public int getStatus() {
        return myImpl.getStatus();
    }

    @Override
    public void setStatus(int status) {
        myImpl.setStatus(status);
    }

    @Override
    public String getStatusText() {
        return myImpl.getStatusText();
    }

    @Override
    public void setStatusText(String statusText) {
        myImpl.setStatusText(statusText);
    }

    @Override
    public String getMimeType() {
        return myImpl.getMimeType();
    }

    @Override
    public void setMimeType(String mimeType) {
        myImpl.setMimeType(mimeType);
    }

    @Override
    public String getHeaderByName(String name) {
        return myImpl.getHeaderByName(name);
    }

    @Override
    public void setHeaderByName(String name, String value, boolean overwrite) {
        myImpl.setHeaderByName(name, value, overwrite);
    }

    @Override
    public void getHeaderMap(Map<String, String> headerMap) {
        myImpl.getHeaderMap(headerMap);
    }

    @Override
    public void setHeaderMap(Map<String, String> headerMap) {
        myImpl.setHeaderMap(headerMap);
    }
}
