package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.misc.CefLog;
import org.cef.security.CefSSLInfo;

public class RemoteSSLInfo extends CefSSLInfo {
    public RemoteSSLInfo(RObject robj) {
        super(0, null);
        // TODO: implement (plain data transfer)
        CefLog.Error("RemoteSSLInfo: unimplemented.");
    }

}
