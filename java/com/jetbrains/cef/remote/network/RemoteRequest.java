package com.jetbrains.cef.remote.network;

import org.cef.network.CefPostData;
import org.cef.network.CefRequest;

import java.util.Map;

public class RemoteRequest extends CefRequest {
    private final RemoteRequestImpl myImpl;

    public RemoteRequest(RemoteRequestImpl impl) {
        super();
        myImpl = impl;
    }

    @Override
    public void dispose() {}

    @Override
    public long getIdentifier() {
        return myImpl.getIdentifier();
    }

    @Override
    public boolean isReadOnly() {
        return myImpl.isReadOnly();
    }

    @Override
    public String getURL() {
        return myImpl.getURL();
    }

    @Override
    public void setURL(String url) {
        myImpl.setURL(url);
    }

    @Override
    public String getMethod() {
        return myImpl.getMethod();
    }

    @Override
    public void setMethod(String method) {
        myImpl.setMethod(method);
    }

    @Override
    public void setReferrer(String url, ReferrerPolicy policy) {
        myImpl.setReferrer(url, policy);
    }

    @Override
    public String getReferrerURL() {
        return myImpl.getReferrerURL();
    }

    @Override
    public ReferrerPolicy getReferrerPolicy() {
        return myImpl.getReferrerPolicy();
    }

    @Override
    public CefPostData getPostData() {
        return myImpl.getPostData();
    }

    @Override
    public void setPostData(CefPostData postData) {
        myImpl.setPostData(postData);
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

    @Override
    public void set(String url, String method, CefPostData postData, Map<String, String> headerMap) {
        myImpl.set(url, method, postData, headerMap);
    }

    @Override
    public int getFlags() {
        return myImpl.getFlags();
    }

    @Override
    public void setFlags(int flags) {
        myImpl.setFlags(flags);
    }

    @Override
    public String getFirstPartyForCookies() {
        return myImpl.getFirstPartyForCookies();
    }

    @Override
    public void setFirstPartyForCookies(String url) {
        myImpl.setFirstPartyForCookies(url);
    }

    @Override
    public ResourceType getResourceType() {
        return myImpl.getResourceType();
    }

    @Override
    public TransitionType getTransitionType() {
        return myImpl.getTransitionType();
    }
}
