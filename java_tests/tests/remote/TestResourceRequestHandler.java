package tests.remote;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefCallback;
import org.cef.handler.CefCookieAccessFilter;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.CefLog;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.cef.network.CefURLRequest;

import java.nio.ByteBuffer;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNull;

public class TestResourceRequestHandler implements CefResourceRequestHandler {
    private static class ResourceContent {
        String content;
        String mimeType;
        HashMap<String, String> headerMap;
    }
    private HashMap<String, ResourceContent> resourceMap_ = null;

    protected void addResource(String url, String content, String mimeType) {
        if (resourceMap_ == null) resourceMap_ = new HashMap<>();
        assertNull(resourceMap_.get(url));

        ResourceContent rc = new ResourceContent();
        rc.content = content;
        rc.mimeType = mimeType;
        rc.headerMap = null;

        resourceMap_.put(url, rc);
    }

    @Override
    public CefCookieAccessFilter getCookieAccessFilter(CefBrowser browser, CefFrame frame, CefRequest request) {
        return new TestCookieAccessFilter();
    }

    @Override
    public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
        CefLog.Info("onBeforeResourceLoad " + browser + ", request:" + request.getURL());
        return false;
    }

    @Override
    public CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame, CefRequest request) {
        if (resourceMap_ != null) {
            String url = request.getURL();

            // Ignore the query component, if any.
            int idx = url.indexOf('?');
            if (idx > 0) url = url.substring(0, idx);

            ResourceContent rc = resourceMap_.get(url);
            if (rc != null) {
                CefLog.Debug("Found resource for: " + url);
                return new CefResourceHandler() {
                    private int offset_ = 0;
                    private final String content_ = rc.content;
                    private final String mimeType_ = rc.mimeType;
                    private final HashMap<String, String> headerMap_ = rc.headerMap;

                    @Override
                    public boolean processRequest(CefRequest request, CefCallback callback) {
                        CefLog.Info("processRequest " + request.getURL());
                        callback.Continue();
                        return true;
                    }

                    @Override
                    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
                        CefLog.Info("getResponseHeaders " + browser);
                        responseLength.set(content_.length());
                        response.setMimeType(mimeType_);
                        response.setStatus(200);

                        if (headerMap_ != null) {
                            HashMap<String, String> headerMap = new HashMap<>();
                            response.getHeaderMap(headerMap);
                            headerMap.putAll(headerMap_);
                            response.setHeaderMap(headerMap);
                        }
                    }

                    @Override
                    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
                        CefLog.Info("readResponse %s, bytesToRead=%d", browser, bytesToRead);
                        int length = content_.length();
                        if (offset_ >= length) return false;

                        // Extract up to |bytes_to_read| bytes from |content_|.
                        int endPos = offset_ + bytesToRead;
                        String dataToSend = (endPos > length) ? content_.substring(offset_)
                                : content_.substring(offset_, endPos);

                        // Copy extracted bytes into |data_out| and set the read length to |bytes_read|.
                        ByteBuffer result = ByteBuffer.wrap(dataOut);
                        result.put(dataToSend.getBytes());
                        bytesRead.set(dataToSend.length());

                        offset_ = endPos;
                        return true;
                    }

                    @Override
                    public void cancel() {
                        CefLog.Info("cancel " + browser);
                    }
                };
            }
        }
        return null;
    }

    @Override
    public void onResourceRedirect(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, StringRef new_url) {
        CefLog.Info("onResourceRedirect " + browser + ", request:" + request.getURL() + ", response:" + response);
    }

    @Override
    public boolean onResourceResponse(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response) {
        CefLog.Info("onResourceResponse " + browser + ", request:" + request.getURL() + ", response:" + response);
        return false;
    }

    @Override
    public void onResourceLoadComplete(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, CefURLRequest.Status status, long receivedContentLength) {
        CefLog.Info("onResourceLoadComplete " + browser + ", request:" + request + ", response:" + response);
    }

    @Override
    public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request, BoolRef allowOsExecution) {
        CefLog.Info("onProtocolExecution " + browser + ", request:" + request);
    }
}
