package com.jetbrains.cef.remote.network;

import org.cef.misc.CefLog;
import org.cef.security.CefSSLInfo;
import org.cef.security.CefX509Certificate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RemoteSSLInfo {
    public static CefSSLInfo fromBinary(ByteBuffer binaryData) {
        binaryData.order(ByteOrder.LITTLE_ENDIAN);
        int statusMask = binaryData.getInt();
        int chainSize = binaryData.getInt();
        if (chainSize == 0) {
            CefLog.Debug("RemoteSSLInfo: chainSize == 0, status=%d", statusMask);
            return null;
        }

        byte[][] chainDERData = new byte[chainSize][];
        for (int c = 0; c < chainSize; ++c) {
            int bytesCount = binaryData.getInt();
            //CefLog.Debug("RemoteSSLInfo: read chunk of %d bytes (c=%d)", bytesCount, c);
            if (bytesCount == 0)
                continue;

            byte[] chainDERDataItem = new byte[bytesCount];
            binaryData.get(chainDERDataItem, 0, bytesCount);
            chainDERData[c] = chainDERDataItem;

            if ((bytesCount % 4) != 0)
                binaryData.get(new byte[4], 0, 4 - (bytesCount % 4));
        }

        return new CefSSLInfo(statusMask, new CefX509Certificate(chainDERData));
    }
}
