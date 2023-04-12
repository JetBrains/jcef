package com.jetbrains.cef.remote;


import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import com.jetbrains.cef.remote.thrift_codegen.Server;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.cef.handler.CefNativeRenderHandler;
import org.cef.misc.CefLog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;

public class CefServer {
    private static int PORT = Integer.getInteger("jcef.remote.port", 9090);

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerSocket myClientHandlersTransport;
    private ClientHandlersImpl myClientHandlersImpl;

    // Java client for CefServer
    private TTransport myTransport;
    private TProtocol myProtocol;
    private Server.Client myCefServerClient;

    public CefRemoteBrowser createBrowser(CefNativeRenderHandler renderHandle) {
        int bid = createRemoteBrowser(renderHandle);
        if (bid < 0)
            return null;

        return new CefRemoteBrowser(this, bid);
    }

    // returns remote browser id (or negative value when error occured)
    private int createRemoteBrowser(CefNativeRenderHandler renderHandle) {
        int result;
        try {
            result = myCefServerClient.createBrowser();
        } catch (TException e) {
            onThriftException(e);
            return -1;
        }
        myClientHandlersImpl.registerRenderHandler(result, renderHandle);
        return result;
    }

    // closes remote browser
    public void closeBrowser(int bid) {
        try {
            // TODO: support force flag
            String err = myCefServerClient.closeBrowser(bid);
            if (err != null && !err.isEmpty())
                CefLog.Error("tried to close remote browser %d, error '%s'", bid, err);
        } catch (TException e) {
            onThriftException(e);
        }

        myClientHandlersImpl.unregister(bid);
    }

    // invokes method of remote browser
    public void invoke(int bid, String method, ByteBuffer params) {
        try {
            myCefServerClient.invoke(bid, method, params);
        } catch (TException e) {
            onThriftException(e);
        }
    }

    // connect to CefServer and start cef-handlers service
    public boolean start() {
        try {
            // 1. Start server for cef-handlers execution
            myClientHandlersImpl = new ClientHandlersImpl();
            ClientHandlers.Processor processor = new ClientHandlers.Processor(myClientHandlersImpl);
            myClientHandlersTransport = new TServerSocket(PORT + 1);
            myClientHandlersServer = new TSimpleServer(new TServer.Args(myClientHandlersTransport).processor(processor));

            CefLog.Debug("Starting cef-handlers server.");
            myClientHandlersThread = new Thread(()->{
                // Use this for a multithreaded server
                // TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
                myClientHandlersServer.serve();
            });
            myClientHandlersThread.setName("CefHandlers-thread");
            myClientHandlersThread.start();

            // 2. Create client and connect to CefServer
            myTransport = new TSocket("localhost", PORT);
            myTransport.open();

            myProtocol = new TBinaryProtocol(myTransport);
            myCefServerClient = new Server.Client(myProtocol);

            int cid = myCefServerClient.connect();
            CefLog.Debug("Connected to CefSever, cid=" + cid);
        } catch (TException x) {
            CefLog.Error("exception in CefServer.start: %s", x.getMessage());
            return false;
        }

        return true;
    }

    public void stop() {
        if (myTransport != null) {
            myTransport.close();
            myTransport = null;
        }
        if (myClientHandlersTransport != null) {
            myClientHandlersTransport.close();
            myClientHandlersTransport = null;
        }

        if (myClientHandlersServer != null) {
            myClientHandlersServer.stop();
            myClientHandlersServer = null;
        }
    }

    private void onThriftException(TException e) {
        CefLog.Error("thrift exception '%s'", e.getMessage());
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        CefLog.Error(sw.getBuffer().toString());

        // TODO: check whether socket is still open and reconnect if necessary
    }
}
