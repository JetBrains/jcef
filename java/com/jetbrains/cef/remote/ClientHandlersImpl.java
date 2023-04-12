package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import org.apache.thrift.TException;
import org.cef.handler.CefLifeSpanHandler;
import org.cef.handler.CefNativeRenderHandler;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.cef.misc.CefLog;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandlersImpl implements ClientHandlers.Iface {
    private final Map<Integer, CefRemoteClient> myCid2RemoteClient = new ConcurrentHashMap<>();

    void registerRemoteClient(CefRemoteClient remoteClient) {
        if (remoteClient == null) {
            CefLog.Error("tried to register null remoteClient");
            return;
        }
        myCid2RemoteClient.put(remoteClient.getCid(), remoteClient);
    }

    public void unregisterClient(int cid) {
        CefRemoteClient client = myCid2RemoteClient.remove(cid);
        if (client != null)
            client.disposeClient();
    }

    public void unregisterBrowser(int cid, int bid) {
        CefRemoteClient client = myCid2RemoteClient.get(cid);
        if (client != null)
            client.unregister(bid);
    }

    @Override
    public int connect() throws TException {
        return 0;
    }

    @Override
    public void log(String msg) throws TException {
        CefLog.Debug("received message from CefServer: " + msg);
    }

    @Override
    public ByteBuffer getInfo(int cid, int bid, String request, ByteBuffer buffer) throws TException {
        CefRemoteClient rc = myCid2RemoteClient.get(cid);
        CefRenderHandler rh = rc != null ? rc.getRenderHandler() : null;
        if (rh == null) {
            CefLog.Error("getInfo, rh == null");
            ByteBuffer result = ByteBuffer.allocate(4);
            result.putInt(0);
            return result;
        }
        CefRemoteBrowser browser = rc.getBrowserByBid(bid);

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
            ByteBuffer result = ByteBuffer.allocate(4);
            result.putInt(0);
            return result;
        }

        ByteBuffer result = ByteBuffer.allocate(data.length*4);
        result.order(ByteOrder.nativeOrder());
        result.asIntBuffer().put(data);
        return result;
    }

    @Override
    public void onPaint(int cid, int bid, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, boolean recreateHandle, int width, int height) throws TException {
        CefRemoteClient rc = myCid2RemoteClient.get(cid);
        if (rc == null) {
            CefLog.Error("onPaint, rc == null");
            return;
        }

        CefNativeRenderHandler rh = (CefNativeRenderHandler)rc.getRenderHandler();
        if (rh == null) {
            CefLog.Error("onPaint, rh == null");
            return;
        }

        CefRemoteBrowser browser = rc.getBrowserByBid(bid);
        rh.onPaintWithSharedMem(browser, popup, dirtyRectsCount, sharedMemName, sharedMemHandle, recreateHandle, width, height);
    }

    private static class LifeSpanHandlerWr {
        CefRemoteClient rc;
        CefLifeSpanHandler lsh;
    };
    private LifeSpanHandlerWr _getCefLifeSpanHandler(int cid) {
        LifeSpanHandlerWr r = new LifeSpanHandlerWr();
        r.rc = myCid2RemoteClient.get(cid);
        if (r.rc == null) {
            CefLog.Error("_getCefLifeSpanHandler, rc == null");
            return null;
        }
        r.lsh = r.rc.getLifeSpanHandler();
        if (r.lsh == null) {
            CefLog.Error("_getCefLifeSpanHandler, lsh == null");
            return null;
        }
        return r;
    }

    @Override
    public void onBeforePopup(int cid, int bid, String url, boolean gesture) throws TException {
        LifeSpanHandlerWr r = _getCefLifeSpanHandler(cid);
        if (r == null) return;

        CefRemoteBrowser browser = r.rc.getBrowserByBid(bid);
        r.lsh.onBeforePopup(browser, null, url, "");
    }

    @Override
    public void onAfterCreated(int cid, int bid) throws TException {
        LifeSpanHandlerWr r = _getCefLifeSpanHandler(cid);
        if (r == null) return;

        CefRemoteBrowser browser = r.rc.getBrowserByBid(bid);
        r.lsh.onAfterCreated(browser);
    }

    @Override
    public void doClose(int cid, int bid) throws TException {
        LifeSpanHandlerWr r = _getCefLifeSpanHandler(cid);
        if (r == null) return;

        CefRemoteBrowser browser = r.rc.getBrowserByBid(bid);
        r.lsh.doClose(browser);
    }

    @Override
    public void onBeforeClose(int cid, int bid) throws TException {
        LifeSpanHandlerWr r = _getCefLifeSpanHandler(cid);
        if (r == null) return;

        CefRemoteBrowser browser = r.rc.getBrowserByBid(bid);
        r.lsh.onBeforeClose(browser);
    }
}
