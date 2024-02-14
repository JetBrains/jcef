package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerTransport;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.handler.CefAppHandler;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CefServer {
    private static final CefServer INSTANCE = CefApp.isRemoteEnabled() ? new CefServer() : null;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerTransport myClientHandlersTransport;
    private final RpcExecutor myService = new RpcExecutor();
    private final Map<Integer, RemoteBrowser> myBid2Browser = new ConcurrentHashMap<>();
    private final ClientHandlersImpl myClientHandlersImpl = new ClientHandlersImpl(myService, myBid2Browser);

    private volatile boolean myIsConnected = false;

    // Connects to CefServer and start cef-handlers service.
    // Should be executed in bg thread.
    public static boolean connect(CefAppHandler appHandler, CefSettings settings) {
        if (!CefApp.isRemoteEnabled())
            return false;

        final long waitTimeoutNs = Utils.getInteger("WAIT_SERVER_TIMEOUT_MS", 15000)*1000000l;
        if (!NativeServerManager.startIfNecessary(appHandler, settings, waitTimeoutNs))
            return false;

        if (!INSTANCE.connect(appHandler::onContextInitialized)) {
            CefLog.Error("Can't initialize client for native server.");
            return false;
        }
        INSTANCE.myIsConnected = true;
        return true;
    }

    public static CefServer instance() { return INSTANCE; }

    public boolean isConnected() { return myIsConnected; }

    public RpcExecutor getService() { return myService; }

    public RemoteClient createClient() {
        return new RemoteClient(myService, myBid2Browser);
    }

    public static String getVersion() {
        if (CefApp.isRemoteEnabled() && INSTANCE.myIsConnected)
            return INSTANCE.myService.execObj(r->r.version());
        return "unknown(not connected)";
    }

    private boolean connect(Runnable onContextInitialized) {
        try {
            myClientHandlersImpl.setOnContextInitialized(onContextInitialized);

            // 1. Start server for cef-handlers execution. Open socket
            CefLog.Debug("Initialize CefServer. Open socket.");
            myService.init("cef_server_pipe");

            // 2. Start service for backward rpc calls (from native to java)
            ClientHandlers.Processor processor = new ClientHandlers.Processor(myClientHandlersImpl);
            myClientHandlersTransport = ThriftTransport.createServerTransport();

            TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(myClientHandlersTransport)
                .processor(processor).executorService(new ThreadPoolExecutor(2, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue(), new ThreadFactory() {
                    final AtomicLong count = new AtomicLong();
                    public Thread newThread(Runnable r) {
                        final String name = String.format("CefHandlers-execution-%d", this.count.getAndIncrement());
                        Thread thread = new Thread(r, name);
                        thread.setDaemon(true);
                        return thread;
                    }
                }));
            myClientHandlersServer = new TThreadPoolServer(serverArgs);
            myClientHandlersThread = new Thread(()-> myClientHandlersServer.serve());
            myClientHandlersThread.setName("CefHandlers-listening");
            myClientHandlersThread.start();

            // 3. Connect to CefServer
            int[] cid = new int[]{-1};
            myService.exec((s)->{
                cid[0] = s.connect(ThriftTransport.PIPENAME);
            });

            CefLog.Debug("Connected to CefSever, cid=" + cid[0]);
        } catch (TException x) {
            CefLog.Error("TException in CefServer.start: %s", x.getMessage());
            return false;
        } catch (IOException e) {
            CefLog.Error("IOException in CefServer.start: %s", e.getMessage());
            return false;
        } catch (RuntimeException e) {
            CefLog.Error("RuntimeException in CefServer.start: %s", e.getMessage());
            return false;
        }

        return true;
    }

    public void disconnect() {
        CefLog.Debug("Disconnect from native server and stop it.");
        myIsConnected = false;

        myService.exec(s->s.stop());
        myService.closeTransport();

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
