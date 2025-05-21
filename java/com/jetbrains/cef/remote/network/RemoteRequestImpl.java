package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RemoteServerObjectLocal;
import com.jetbrains.cef.remote.thrift_codegen.PostData;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.misc.CefLog;
import org.cef.network.CefPostData;
import org.cef.network.CefRequest;

import java.util.Map;

// 1. Represents remote java peer for native server object (CefRequest).
// 2. Created
//      a) on java side when processing some server request (and object will be invalid after rpc finished)
//      b) on java side when user creates custom CefRequest (object will be valid until GC)
// 3. Lifetime of remote native peer is managed by java: native object
// peer will be destroyed when java object destroyed via usual gc.
public class RemoteRequestImpl extends RemoteServerObject {
    public RemoteRequestImpl(RpcContext rpcContext, RObject request) {
        super(rpcContext, request);
    }

    @Override
    protected void disposeOnServerImpl() {
        myRpc.invokeLater(s -> s.Request_Dispose(myId));
    }

    @Override
    public void flush() {
        myRpc.exec((s)->{
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
        setStrVal("ReferrerPolicy", policy == null ? null : policy.name());
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
        PostData pd = myRpc.execObj((s)-> s.Request_GetPostData(thriftId()));
        return pd == null || pd.isNull ? null : new RemotePostData(pd);
    }

    public void setPostData(CefPostData postData) {
        myRpc.exec((s)->{
            s.Request_SetPostData(thriftId(), RemotePostData.toThriftWithMap(postData));
        });
    }

    public void set(String url, String method, CefPostData postData, Map<String, String> headerMap) {
        myRpc.exec((s)->{
            s.Request_Set(thriftId(), url, method, RemotePostData.toThriftWithMap(postData), headerMap);
        });
    }

    public String getHeaderByName(String name) {
        return myRpc.execObj((s)->s.Request_GetHeaderByName(thriftId(), name));
    }

    public void setHeaderByName(String name, String value, boolean overwrite) {
        myRpc.exec((s)->{
            s.Request_SetHeaderByName(thriftId(), name, value, overwrite);
        });
    }

    public void getHeaderMap(Map<String, String> headerMap) {
        if (headerMap == null)
            return;
        Map<String, String> result = myRpc.execObj((s)-> s.Request_GetHeaderMap(thriftId()));
        if (result != null)
            headerMap.putAll(result);
    }

    public void setHeaderMap(Map<String, String> headerMap) {
        myRpc.exec((s)->{
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
        final int TT_SOURCE_MASK = 0xFF;
        final int rawVal = (int)getLongVal("TransitionType");
        final int source = rawVal & TT_SOURCE_MASK;
        CefRequest.TransitionType result = null;
        switch (source) {
            case 0:
                /// Source is a link click or the JavaScript window.open function. This is
                /// also the default value for requests like sub-resource loads that are not
                /// navigations.
                ///
                // TT_LINK = 0,
                result = CefRequest.TransitionType.TT_LINK;
                break;
            case 1:
                /// Source is some other "explicit" navigation. This is the default value for
                /// navigations where the actual type is unknown. See also
                /// TT_DIRECT_LOAD_FLAG.
                ///
                //TT_EXPLICIT = 1,
                result = CefRequest.TransitionType.TT_EXPLICIT;
                break;
            case 3:
                /// Source is a subframe navigation. This is any content that is automatically
                /// loaded in a non-toplevel frame. For example, if a page consists of several
                /// frames containing ads, those ad URLs will have this transition type.
                /// The user may not even realize the content in these pages is a separate
                /// frame, so may not care about the URL.
                ///
                //TT_AUTO_SUBFRAME = 3,
                result = CefRequest.TransitionType.TT_AUTO_SUBFRAME;
                break;
            case 4:
                /// Source is a subframe navigation explicitly requested by the user that will
                /// generate new navigation entries in the back/forward list. These are
                /// probably more important than frames that were automatically loaded in
                /// the background because the user probably cares about the fact that this
                /// link was loaded.
                ///
                //TT_MANUAL_SUBFRAME = 4,
                result = CefRequest.TransitionType.TT_MANUAL_SUBFRAME;
                break;
            case 7:
                ///
                /// Source is a form submission by the user. NOTE: In some situations
                /// submitting a form does not result in this transition type. This can happen
                /// if the form uses a script to submit the contents.
                ///
                //TT_FORM_SUBMIT = 7,
                result = CefRequest.TransitionType.TT_FORM_SUBMIT;
                break;
            case 8:
                /// Source is a "reload" of the page via the Reload function or by re-visiting
                /// the same URL. NOTE: This is distinct from the concept of whether a
                /// particular load uses "reload semantics" (i.e. bypasses cached data).
                ///
                //TT_RELOAD = 8,
                result = CefRequest.TransitionType.TT_RELOAD;
                break;
            default:
                CefLog.Debug("getTransitionType: can't find TransitionType enum: nval=%d, source=%d", rawVal, source);
        }

        if (result != null) {
            final int TT_QUALIFIER_MASK = 0xFFFFFF00;
            final int qualifiers = rawVal & TT_QUALIFIER_MASK;
            result.addQualifiers(qualifiers);
        }

        return result;
    }

    @Override
    public String toString() {
        // TODO: use return CefRequest.toString(this) after debugging
        return myCache.toString();
    }
}
