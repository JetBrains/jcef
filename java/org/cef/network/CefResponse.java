package org.cef.network;

import org.cef.handler.CefLoadHandler;

import java.util.Map;

public interface CefResponse {
    /**
     * Returns true if this object is read-only.
     */
     boolean isReadOnly();

    /**
     * Get the response error code. Returns ERR_NONE if there was no error.
     */
     CefLoadHandler.ErrorCode getError();

    /**
     * Get the response error code. Returns ERR_NONE if there was no error.
     */
     void setError(CefLoadHandler.ErrorCode errorCode);

    /**
     * Get the response status code.
     */
     int getStatus();

    /**
     * Set the response status code.
     */
     void setStatus(int status);

    /**
     * Get the response status text.
     */
     String getStatusText();

    /**
     * Set the response status text.
     */
     void setStatusText(String statusText);

    /**
     * Get the response mime type.
     */
     String getMimeType();

    /**
     * Set the response mime type.
     */
     void setMimeType(String mimeType);

    /**
     * Get the value for the specified response header field. Use getHeaderMap instead if there
     * might be multiple values.
     * @param name The header name.
     * @return The header value.
     */
     String getHeaderByName(String name);

    /**
     * Set the value for the specified response header field.
     * @param name The header name.
     * @param value The header value.
     * @param overwrite If true any existing values will be replaced with the new value. If false
     *         any existing values will not be overwritten.
     */
     void setHeaderByName(String name, String value, boolean overwrite);

    /**
     * Get all response header fields.
     */
     void getHeaderMap(Map<String, String> headerMap);

    /**
     * Set all response header fields.
     */
     void setHeaderMap(Map<String, String> headerMap);
}
