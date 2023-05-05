package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import com.jetbrains.cef.remote.thrift_codegen.CustomScheme;
import org.apache.thrift.TException;
import org.cef.handler.*;
import org.cef.misc.CefLog;
import org.cef.network.CefRequest;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//
// Service for rpc from native to java
//
public class ClientHandlersImpl implements ClientHandlers.Iface {
    private final Map<Integer, CefRemoteBrowser> myBid2RemoteBrowser = new ConcurrentHashMap<>();
    private final CefRemoteApp myRemoteApp;
    private final Server.Client myServer;

    public ClientHandlersImpl(Server.Client server, CefRemoteApp remoteApp) {
        myRemoteApp = remoteApp;
        myServer = server;
    }

    void registerBrowser(CefRemoteBrowser browser) {
        myBid2RemoteBrowser.put(browser.getBid(), browser);
    }

    public void unregisterBrowser(int bid) {
        CefRemoteBrowser browser = myBid2RemoteBrowser.remove(bid);
        if (browser == null) {
            CefLog.Error("unregisterBrowser: bid=%d was already removed.");
            return;
        }
        browser.getOwner().disposeClient();
    }

    private CefRemoteBrowser getRemoteBrowser(int bid) {
        CefRemoteBrowser browser = myBid2RemoteBrowser.get(bid);
        if (browser == null) {
            CefLog.Error("Can't find remote browser with bid=%d.", bid);
            return null;
        }
        return browser;
    }

    @Override
    public int connect() throws TException {
        return 0;
    }

    @Override
    public void log(String msg) throws TException {
        CefLog.Debug("received message from CefServer: " + msg);
    }

    //
    // CefApp
    //

    @Override
    public void onContextInitialized() {
        CefLog.Debug("onContextInitialized: ");
        myRemoteApp.onContextInitialized();
    }

    @Override
    public List<CustomScheme> getRegisteredCustomSchemes() {
        CefLog.Debug("onRegisterCustomSchemes: ");
        return myRemoteApp.getAllRegisteredCustomSchemes();
    }

    //
    // CefRenderHandler
    //

    private static final ByteBuffer ZERO_BUFFER = ByteBuffer.allocate(4).putInt(0);

    @Override
    public ByteBuffer getInfo(int bid, String request, ByteBuffer buffer) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return ZERO_BUFFER;

        CefRemoteClient rclient = browser.getOwner();
        CefRenderHandler rh = rclient.getRenderHandler();
        if (rh == null) return ZERO_BUFFER;

        request = request == null ? "" : request.toLowerCase();
        int[] data = new int[0];
        if ("viewrect".equals(request)) {
            Rectangle rect = rh.getViewRect(browser);
            data = new int[]{rect.x, rect.y, rect.width, rect.height};
        } else if ("screeninfo".equals(request)) {
            CefScreenInfo csi = new CefScreenInfo();
            boolean success = rh.getScreenInfo(browser, csi);
            if (success) {
                data = new int[]{
                        (int) csi.device_scale_factor,
                        csi.depth,
                        csi.depth_per_component,
                        csi.is_monochrome ? 1 : 0,
                        csi.x,
                        csi.y,
                        csi.width,
                        csi.height,
                        csi.available_x,
                        csi.available_y,
                        csi.available_width,
                        csi.available_height
                };
            } else {
                data = new int[]{0};
            }
        } else if ("screenpoint".equals(request)) {
            Point pt = new Point(buffer.getInt(), buffer.getInt());
            Point res = rh.getScreenPoint(browser, pt);
            data = new int[]{res.x, res.y};
        } else {
            CefLog.Error("getInfo, unknown request: " + request);
            return ZERO_BUFFER;
        }

        ByteBuffer result = ByteBuffer.allocate(data.length*4);
        result.order(ByteOrder.nativeOrder());
        result.asIntBuffer().put(data);
        return result;
    }

    @Override
    public void onPaint(int bid, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, boolean recreateHandle, int width, int height) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefRemoteClient rc = browser.getOwner();
        CefRenderHandler rh = rc.getRenderHandler();
        if (rh == null) return;

        ((CefNativeRenderHandler)rh).onPaintWithSharedMem(browser, popup, dirtyRectsCount, sharedMemName, sharedMemHandle, recreateHandle, width, height);
    }

    //
    // CefLifeSpanHandler
    //

    @Override
    public void onBeforePopup(int bid, String url, String frameName, boolean gesture) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLifeSpanHandler lsh = browser.getOwner().getLifeSpanHandler();
        if (lsh == null) return;

        lsh.onBeforePopup(browser, null, url, frameName);
    }

    @Override
    public void onAfterCreated(int bid) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLifeSpanHandler lsh = browser.getOwner().getLifeSpanHandler();
        if (lsh == null) return;

        lsh.onAfterCreated(browser);
    }

    @Override
    public void doClose(int bid) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLifeSpanHandler lsh = browser.getOwner().getLifeSpanHandler();
        if (lsh == null) return;

        lsh.doClose(browser);
    }

    @Override
    public void onBeforeClose(int bid) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLifeSpanHandler lsh = browser.getOwner().getLifeSpanHandler();
        if (lsh == null) return;

        lsh.onBeforeClose(browser);
    }


    //
    // CefLoadHandler
    //

    @Override
    public void onLoadingStateChange(int bid, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLoadHandler lh = browser.getOwner().getLoadHandler();
        if (lh == null) return;

        lh.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
    }

    @Override
    public void onLoadStart(int bid, int transition_type) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLoadHandler lh = browser.getOwner().getLoadHandler();
        if (lh == null) return;

        // TODO: use correct transition_type instead of TT_LINK
        lh.onLoadStart(browser, null, CefRequest.TransitionType.TT_LINK);
    }

    @Override
    public void onLoadEnd(int bid, int httpStatusCode) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLoadHandler lh = browser.getOwner().getLoadHandler();
        if (lh == null) return;

        lh.onLoadEnd(browser, null, httpStatusCode);
    }

    @Override
    public void onLoadError(int bid, int errorCode, String errorText, String failedUrl) {
        CefRemoteBrowser browser = getRemoteBrowser(bid);
        if (browser == null) return;

        CefLoadHandler lh = browser.getOwner().getLoadHandler();
        if (lh == null) return;

        lh.onLoadError(browser, null, CefLoadHandler.ErrorCode.findByCode(errorCode), errorText, failedUrl);
    }
}
