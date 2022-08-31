package com.jetbrains.cef.remote;


import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.cef.misc.CefLog;

import java.awt.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CefServer {
    private static int PORT = Integer.getInteger("jcef.remote.port", 9090);

    // Fields for cef-handlers execution on java side
    private TTransport myTransport;
    private TProtocol myProtocol;
    private Thread myThread;
    private TServer myJavaCallbackServer;

    // Java client for CefServer
    private Server.Client myClient;

    private final Map<Integer, CefRenderHandler> myBid2RenderHandle = new HashMap<>();

    public int createBrowser(CefRenderHandler renderHandle) {
        int result;
        try {
            result = myClient.createBrowser();
        } catch (TException e) {
            e.printStackTrace();
            return -1;
        }
        myBid2RenderHandle.put(result, renderHandle);
        return result;
    }

    public void invoke(int bid, String method, ByteBuffer params) {
        try {
            myClient.invoke(bid, method, params);
        } catch (TException e) {
            e.printStackTrace();
        }
    }

    public class RenderHandler implements ClientHandlers.Iface {
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
            CefRenderHandler rh = myBid2RenderHandle.get(bid);
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
        public void onPaint(int bid, boolean popup, ByteBuffer dirtyRects, ByteBuffer buffer, int width, int height) throws TException {
            if (dirtyRects == null) {
                CefLog.Error("onPaint, dirtyRects == null");
                return;
            }

            CefRenderHandler rh = myBid2RenderHandle.get(bid);
            if (rh == null) {
                CefLog.Error("onPaint, rh == null");
                return;
            }

            ArrayList<Rectangle> rects = new ArrayList<>();
            while (dirtyRects.hasRemaining()) {
                try {
                    Rectangle r = new Rectangle(dirtyRects.getInt(), dirtyRects.getInt(), dirtyRects.getInt(), dirtyRects.getInt());
                    rects.add(r);
                } catch (BufferUnderflowException e) {
                    break;
                }
            }

            rh.onPaint(null, popup, rects.toArray(new Rectangle[0]), buffer, width, height);
        }
    }

    public void start() {
        try {
            // 1. Start server for cef-handlers execution
            ClientHandlers.Processor processor = new ClientHandlers.Processor(new RenderHandler());
            try {
                TServerTransport serverTransport = new TServerSocket(PORT + 1);
                myJavaCallbackServer = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));
            } catch (Exception e) {
                e.printStackTrace();
            }

            myThread = new Thread(()->{
                // Use this for a multithreaded server
                // TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
                myJavaCallbackServer.serve();
            });
            myThread.setName("CefHandlers-thread");
            CefLog.Debug("Starting cef-handlers server.");
            myThread.start();

            // 2. Create client and connect to CefServer
            myTransport = new TSocket("localhost", PORT);
            myTransport.open();

            myProtocol = new TBinaryProtocol(myTransport);
            myClient = new Server.Client(myProtocol);

            int cid = myClient.connect();
            CefLog.Debug("Connected to CefSever, cid=" + cid);
        } catch (TException x) {
            x.printStackTrace();
        }
    }

    public void stop() {
        if (myTransport != null) {
            myTransport.close();
            myTransport = null;
        }
    }
}
