package com.jetbrains.cef.remote;


import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.cef.CefSettings;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.misc.CefLog;

import java.util.Collections;
import java.util.List;

public class CefServer {
    private static final int PORT = Integer.getInteger("jcef.remote.port", 9090);
    private static CefServer INSTANCE = null;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerSocket myClientHandlersTransport;
    private ClientHandlersImpl myClientHandlersImpl;

    // Java client for native CefServer
    private final RpcExecutor myService = new RpcExecutor();

    public static void initialize() {
        if (INSTANCE != null)
            return;

        INSTANCE = new CefServer();

        List<String> cefArgs = Collections.emptyList();
        CefSettings settings = new CefSettings();
        if (!INSTANCE.start(cefArgs, settings)) {
            CefLog.Error("Can't connect to CefServer");
            INSTANCE = null;
        }
    }

    public static CefServer instance() { return INSTANCE; }

    public RpcExecutor getService() { return myService; }

    public RemoteClient createClient() {
        return new RemoteClient(myService, myClientHandlersImpl);
    }

    // connect to CefServer and start cef-handlers service
    public boolean start(List<String> args, CefSettings settings) {
        try {
            // 1. Start server for cef-handlers execution
            RemoteApp cefRemoteApp = new RemoteApp() {
                @Override
                public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
                    CefLog.Info("onRegisterCustomSchemes: " + registrar);
                }

                @Override
                public void onContextInitialized() {
                    CefLog.Info("onContextInitialized: ");
                }
            };

            // 1. Create client and open socket
            myService.init(PORT);

            // 2. Start service for backward rpc calls (from native to java)
            myClientHandlersImpl = new ClientHandlersImpl(myService, cefRemoteApp);
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
