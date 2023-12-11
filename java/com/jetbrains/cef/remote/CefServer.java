package com.jetbrains.cef.remote;


import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.misc.CefLog;

import java.util.Collections;
import java.util.List;

public class CefServer {
    private static final int PORT = Integer.getInteger("jcef.remote.port", 9090);
    private static final CefServer INSTANCE = CefApp.isRemoteEnabled() ? new CefServer() : null;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerSocket myClientHandlersTransport;
    private final RpcExecutor myService = new RpcExecutor();
    private final ClientHandlersImpl myClientHandlersImpl = new ClientHandlersImpl(myService, new RemoteApp() {
        @Override
        public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
            CefLog.Info("onRegisterCustomSchemes: " + registrar);
        }
        @Override
        public void onContextInitialized() {
            CefLog.Info("onContextInitialized: ");
        }
    });
    private volatile boolean myIsInitialized = false;

    // Connects to CefServer and start cef-handlers service.
    // Should be executed in bg thread.
    public static boolean initialize() {
        if (!CefApp.isRemoteEnabled())
            return false;

        // TODO: pass args and settings
        List<String> cefArgs = Collections.emptyList();
        CefSettings settings = new CefSettings();
        if (!INSTANCE.initialize(cefArgs, settings)) {
            CefLog.Error("Can't initialize client for CefServer");
            return false;
        }
        INSTANCE.myIsInitialized = true;
        return true;
    }

    public static CefServer instance() { return INSTANCE; }

    public boolean isInitialized() { return myIsInitialized; }

    public RpcExecutor getService() { return myService; }

    public RemoteClient createClient() {
        return new RemoteClient(myService, myClientHandlersImpl);
    }

    private boolean initialize(List<String> args, CefSettings settings) {
        try {
            // 1. Start server for cef-handlers execution. Open socket
            CefLog.Debug("Initialize CefServer. Open socket.");
            myService.init(PORT);

            // 2. Start service for backward rpc calls (from native to java)
            ClientHandlers.Processor processor = new ClientHandlers.Processor(myClientHandlersImpl);
            int backwardConnectionPort = PORT + 1;
            myClientHandlersTransport = new TServerSocket(backwardConnectionPort);
            myClientHandlersServer = new TSimpleServer(new TServer.Args(myClientHandlersTransport).processor(processor));
            CefLog.Debug("Starting cef-handlers server.");
            myClientHandlersThread = new Thread(()->{
                // Use this for a multithreaded server
                // TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
                myClientHandlersServer.serve();
            });
            myClientHandlersThread.setName("CefHandlers-thread");
            myClientHandlersThread.start();

            // 3. Connect to CefServer
            int[] cid = new int[]{-1};
            myService.exec((s)->{
                cid[0] = s.connect(backwardConnectionPort, args, settings.toMap());
            });

            CefLog.Debug("Connected to CefSever, cid=" + cid[0]);
        } catch (TException x) {
            CefLog.Error("exception in CefServer.start: %s", x.getMessage());
            return false;
        }

        return true;
    }

    public void stop() {
        myService.closeSocket();
        if (myClientHandlersTransport != null) {
            myClientHandlersTransport.close();
            myClientHandlersTransport = null;
        }
        if (myClientHandlersServer != null) {
            myClientHandlersServer.stop();
            myClientHandlersServer = null;
        }
    }
}
