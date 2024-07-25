package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.CefApp;
import org.cef.misc.CefLog;
import org.cef.network.CefPostData;
import org.cef.network.CefRequest;

import java.util.Map;

public class RemoteRequest extends CefRequest {
    private RemoteRequestImpl myImpl;

    public RemoteRequest(RemoteRequestImpl impl) {
        super();
        myImpl = impl;
    }

    public RemoteRequest() {
        super();
        CefServer.instance().onConnected(()-> {
            RpcExecutor service = CefServer.instance().getService();
            RObject remoteRequest = service.execObj(s -> s.Request_Create());
            if (remoteRequest.objId < 0) {
                CefLog.Error("Request_Create returns invalid objId %d.", remoteRequest.objId);
                return;
            }
            myImpl = new RemoteRequestImpl(service, remoteRequest);
        }, "Request_Create", false);
    }

    public RemoteRequestImpl getImpl() { return myImpl; }

    @Override
    public void dispose() {}

    @Override
    public long getIdentifier() {
        if (myImpl == null)
            return -1;
        return myImpl.getIdentifier();
    }

    @Override
    public boolean isReadOnly() {
        if (myImpl == null)
            return true;
        return myImpl.isReadOnly();
    }

    @Override
    public String getURL() {
        if (myImpl == null)
            return null;
        return myImpl.getURL();
    }

    @Override
    public void setURL(String url) {
        if (myImpl == null)
            return;
        myImpl.setURL(url);
    }

    @Override
    public String getMethod() {
        if (myImpl == null)
            return null;
        return myImpl.getMethod();
    }

    @Override
    public void setMethod(String method) {
        if (myImpl == null)
            return;
        myImpl.setMethod(method);
    }

    @Override
    public void setReferrer(String url, ReferrerPolicy policy) {
        if (myImpl == null)
            return;
        myImpl.setReferrer(url, policy);
    }

    @Override
    public String getReferrerURL() {
        if (myImpl == null)
            return null;
        return myImpl.getReferrerURL();
    }

    @Override
    public ReferrerPolicy getReferrerPolicy() {
        if (myImpl == null)
            return null;
        return myImpl.getReferrerPolicy();
    }

    @Override
    public CefPostData getPostData() {
        if (myImpl == null)
            return null;
        return myImpl.getPostData();
    }

    @Override
    public void setPostData(CefPostData postData) {
        if (myImpl == null)
            return;
        myImpl.setPostData(postData);
    }

    @Override
    public String getHeaderByName(String name) {
        if (myImpl == null)
            return null;
        return myImpl.getHeaderByName(name);
    }

    @Override
    public void setHeaderByName(String name, String value, boolean overwrite) {
        if (myImpl == null)
            return;
        myImpl.setHeaderByName(name, value, overwrite);
    }

    @Override
    public void getHeaderMap(Map<String, String> headerMap) {
        if (myImpl == null)
            return;
        myImpl.getHeaderMap(headerMap);
    }

    @Override
    public void setHeaderMap(Map<String, String> headerMap) {
        if (myImpl == null)
            return;
        myImpl.setHeaderMap(headerMap);
    }

    @Override
    public void set(String url, String method, CefPostData postData, Map<String, String> headerMap) {
        if (myImpl == null)
            return;
        myImpl.set(url, method, postData, headerMap);
    }

    @Override
    public int getFlags() {
        if (myImpl == null)
            return 0;
        return myImpl.getFlags();
    }

    @Override
    public void setFlags(int flags) {
        if (myImpl == null)
            return;
        myImpl.setFlags(flags);
    }

    @Override
    public String getFirstPartyForCookies() {
        if (myImpl == null)
            return null;
        return myImpl.getFirstPartyForCookies();
    }

    @Override
    public void setFirstPartyForCookies(String url) {
        if (myImpl == null)
            return;
        myImpl.setFirstPartyForCookies(url);
    }

    @Override
    public ResourceType getResourceType() {
        if (myImpl == null)
            return null;
        return myImpl.getResourceType();
    }

    @Override
    public TransitionType getTransitionType() {
        if (myImpl == null)
            return null;
        return myImpl.getTransitionType();
    }
}
