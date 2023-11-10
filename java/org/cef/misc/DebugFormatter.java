package org.cef.misc;

import org.cef.network.CefPostData;
import org.cef.network.CefPostDataElement;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class DebugFormatter {
    static public String toString_PostData(String mimeType, CefPostData pd) {
        Vector<CefPostDataElement> elements = new Vector<CefPostDataElement>();
        pd.getElements(elements);

        String returnValue = "";
        for (CefPostDataElement el : elements) {
            returnValue += toString_PostDataElement(mimeType, el) + "\n";
        }
        return returnValue;
    }

    static public String toString_PostDataElement(String mimeType, CefPostDataElement e) {
        int bytesCnt = e.getBytesCount();
        byte[] bytes = null;
        if (bytesCnt > 0) {
            bytes = new byte[bytesCnt];
        }

        boolean asText = false;
        if (mimeType != null) {
            if (mimeType.startsWith("text/"))
                asText = true;
            else if (mimeType.startsWith("application/xml"))
                asText = true;
            else if (mimeType.startsWith("application/xhtml"))
                asText = true;
            else if (mimeType.startsWith("application/x-www-form-urlencoded"))
                asText = true;
        }

        String returnValue = "";

        if (e.getType() == CefPostDataElement.Type.PDE_TYPE_BYTES) {
            int setBytes = e.getBytes(bytes.length, bytes);
            returnValue += "    Content-Length: " + bytesCnt + "\n";
            if (asText) {
                returnValue += "\n    " + new String(bytes);
            } else {
                for (int i = 0; i < setBytes; i++) {
                    if (i % 40 == 0) returnValue += "\n    ";
                    returnValue += String.format("%02X", bytes[i]) + " ";
                }
            }
            returnValue += "\n";
        } else if (e.getType() == CefPostDataElement.Type.PDE_TYPE_FILE) {
            returnValue += "\n    Bytes of file: " + e.getFile() + "\n";
        }
        return returnValue;
    }

    static public String toString_Request(CefRequest request) {
        String returnValue = "\nHTTP-Request";
        returnValue += "\n  flags: " + request.getFlags();
        returnValue += "\n  resourceType: " + request.getResourceType();
        returnValue += "\n  transitionType: " + request.getTransitionType();
        returnValue += "\n  firstPartyForCookies: " + request.getFirstPartyForCookies();
        returnValue += "\n  referrerURL: " + request.getReferrerURL();
        returnValue += "\n  referrerPolicy: " + request.getReferrerPolicy();
        returnValue += "\n    " + request.getMethod() + " " + request.getURL() + " HTTP/1.1\n";

        Map<String, String> headerMap = new HashMap<>();
        request.getHeaderMap(headerMap);
        Set<Map.Entry<String, String>> entrySet = headerMap.entrySet();
        String mimeType = null;
        for (Map.Entry<String, String> entry : entrySet) {
            String key = entry.getKey();
            returnValue += "    " + key + "=" + entry.getValue() + "\n";
            if (key.equals("Content-Type")) {
                mimeType = entry.getValue();
            }
        }

        CefPostData pd = request.getPostData();
        if (pd != null) {
            returnValue += DebugFormatter.toString_PostData(mimeType, pd);
        }

        return returnValue;
    }

    static public String toString_Response(CefResponse response) {
        String returnValue = "\nHTTP-Response:";

        returnValue += "\n  error: " + response.getError();
        returnValue += "\n  readOnly: " + response.isReadOnly();
        returnValue += "\n    HTTP/1.1 " + response.getStatus() + " " + response.getStatusText();
        returnValue += "\n    Content-Type: " + response.getMimeType();

        Map<String, String> headerMap = new HashMap<>();
        response.getHeaderMap(headerMap);
        Set<Map.Entry<String, String>> entrySet = headerMap.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            returnValue += "    " + entry.getKey() + "=" + entry.getValue() + "\n";
        }

        return returnValue;
    }
}
