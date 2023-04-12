package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import org.apache.thrift.TException;
import org.cef.handler.CefNativeRenderHandler;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.cef.misc.CefLog;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class ClientHandlersImpl implements ClientHandlers.Iface {
    private final Map<Integer, CefNativeRenderHandler> myBid2RenderHandler = new HashMap<>();

    void registerRenderHandler(int bid, CefNativeRenderHandler handler) {
        if (handler == null) {
            CefLog.Error("tried to register null render handler, bid=%d", bid);
            return;
        }
        myBid2RenderHandler.put(bid, handler);
    }

    public void unregister(int bid) {
        CefNativeRenderHandler rh = myBid2RenderHandler.remove(bid);
        if (rh != null)
            rh.disposeNativeResources();
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
    public ByteBuffer getInfo(int bid, String request, ByteBuffer buffer) throws TException {
        CefRenderHandler rh = myBid2RenderHandler.get(bid);
        if (rh == null) {
            CefLog.Error("getInfo, rh == null");
            ByteBuffer result = ByteBuffer.allocate(4);
            result.putInt(0);
            return result;
        }

        request = request == null ? "" : request.toLowerCase();
        int[] data = new int[0];
        if ("viewrect".equals(request)) {
            Rectangle rect = rh.getViewRect(null);
            data = new int[]{rect.x, rect.y, rect.width, rect.height};
        } else if ("screeninfo".equals(request)) {
            CefScreenInfo csi = new CefScreenInfo();
            boolean success = rh.getScreenInfo(null, csi);
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
            Point res = rh.getScreenPoint(null, pt);
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
    public void onPaint(int bid, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, boolean recreateHandle, int width, int height) throws TException {
        CefNativeRenderHandler rh = myBid2RenderHandler.get(bid);
        if (rh == null) {
            CefLog.Error("onPaint, rh == null");
            return;
        }

        rh.onPaintWithSharedMem(null, popup, dirtyRectsCount, sharedMemName, sharedMemHandle, recreateHandle, width, height);
    }
}
