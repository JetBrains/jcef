package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.browser.RemoteBrowser;
import com.jetbrains.cef.remote.browser.RemoteClient;
import com.jetbrains.cef.remote.thrift.TException;
import com.jetbrains.cef.remote.thrift.server.TServer;
import com.jetbrains.cef.remote.thrift.server.TThreadPoolServer;
import com.jetbrains.cef.remote.thrift.transport.TServerTransport;
import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.handler.CefAppHandler;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CefServer {
    private static final Integer WAIT_FOR_SERVER_EXIT_SEC = Utils.getInteger("JCEF_WAIT_FOR_SERVER_EXIT_SEC", 10);
    private static final boolean DONT_STOP_SERVER_MANUALLY = Utils.getBoolean("JCEF_DONT_STOP_SERVER_MANUALLY"); // TODO: remove after platform tests debugging
    private static final boolean DONT_USE_UNIQUE_ROOTS = Utils.getBoolean("JCEF_DONT_USE_UNIQUE_ROOTS"); // TODO: remove after platform tests debugging
    private final ThriftTransport myThriftServer;
    private final ThriftTransport myThriftBackward;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerTransport myClientHandlersTransport;
    private RpcContext myRpc = new RpcContext();
    private final Map<Integer, RemoteBrowser> myBid2Browser = new ConcurrentHashMap<>();
    private final ClientHandlersImpl myClientHandlersImpl = new ClientHandlersImpl(myRpc, myBid2Browser);

    private volatile boolean myIsConnected = false;
    private volatile boolean myIsContextInitialized = false;

    private final LinkedList<Runnable> myDelayedActions = new LinkedList<>();

    public CefServer(ThriftTransport thriftServer, ThriftTransport thriftBackward) {
        myThriftServer = thriftServer;
        myThriftBackward = thriftBackward;
    }

    public ThriftTransport getThriftServer() { return myThriftServer; }
    public ThriftTransport getThriftBackward() { return myThriftBackward; }

    public static CefServer createDefault() { return new CefServer(ThriftTransport.ourDefaultServer, ThriftTransport.ourDefaultClient); }

    public boolean start(CefAppHandler appHandler, CefSettings settings) {
        return start(appHandler, settings, true);
    }

    // Connects to CefServer and start cef-handlers service.
    // Should be executed in bg thread.
    // NOTE: appHandler is necessary for (1) cmdLineArgs, (2) custom schemes, (3) onContextInitialized callback
    public boolean start(CefAppHandler appHandler, CefSettings settings, boolean fixRoot) {
        if (!CefApp.isRemoteEnabled())
            return false;
        if (appHandler == null) { // just for simplicity
            CefLog.Error("Can't initialize client for native server because CefAppHandler is null.");
            return false;
        }

        final String prevRoot = NativeServerManager.isRunning(myThriftServer);
        if (prevRoot != null) {
            // Shouldn't be here because pipe-names are unique for each client process.
            CefLog.Error("Found running cef_server instance with root '%s'", prevRoot);
        } else {
            boolean deleteRoot = false;
            if (fixRoot && !DONT_USE_UNIQUE_ROOTS)
                deleteRoot = NativeServerManager.fixRootInSettings(settings, "cef_cache" + ThriftTransport.getUniqueSuffix());
            final long waitTimeoutMs = Utils.getInteger("WAIT_SERVER_TIMEOUT_MS", 15000);
            final boolean success = NativeServerManager.startProcessAndWait(myThriftServer, appHandler, settings, deleteRoot, waitTimeoutMs);
            if (!success)
                return false;
        }

        if (!connect(appHandler::onContextInitialized)) {
            CefLog.Error("Can't initialize client for native server.");
            return false;
        }
        return true;
    }

    // returns true when server is connected and action was executed immediately
    public boolean onConnected(Runnable r, String name, boolean first) {
        synchronized (myDelayedActions) {
            if (myIsConnected) {
                if (r != null)
                    r.run();
                return true;
            }
            if (r != null) {
                if (first)
                    myDelayedActions.addFirst(r);
                else
                    myDelayedActions.addLast(r);
                CefLog.Debug("Delay action '%s' until server connected (first=%s).", name, String.valueOf(first));
            }
            return false;
        }
    }

    public RpcContext getRpcContext() { return myRpc; }

    public RemoteClient createClient() {
        return new RemoteClient(myRpc, myBid2Browser);
    }

    public static String getVersion() {
        if (CefApp.isRemoteEnabled() && CefApp.getInstance().getServer().myIsConnected)
            return CefApp.getInstance().getServer().myRpc.main.execObj(r->r.getServerInfo("version"));
        return "unknown(not connected)";
    }

    private boolean connect(Runnable onContextInitialized) {
        myClientHandlersImpl.setOnContextInitialized(() -> {
            myIsContextInitialized = true;
            if (onContextInitialized != null)
                onContextInitialized.run();
        });

        try {
            // 1. Start server for cef-handlers execution. Open transport for rpc-handlers
            try {
                CefLog.Debug("Initialize CefServer, open server transport.");
                myRpc.openTransport(myThriftServer);
            } catch (TException x) {
                CefLog.Error("TException when opening server %s : %s", myThriftServer.isTcp() ? "tcp-socket" : "pipe", x.getMessage());
                return false;
            }

            CefLog.Info("cef_server version: %s", (String)myRpc.main.execObj(r->r.getServerInfo("version")));

            // 2. Start service for backward rpc calls (from native to java)
            try {
                myClientHandlersTransport = myThriftBackward.createServerTransport();
            } catch (Exception e) {
                CefLog.Error("Exception when opening client %s : %s", myThriftBackward.isTcp() ? "tcp-socket" : "pipe", e.getMessage());
                if (myThriftBackward.isTcp())
                    CefLog.Error("Port : %d", myThriftBackward.getPort());
                else
                    CefLog.Error("Pipe : %s", myThriftBackward.getPipe());
                return false;
            }

            ClientHandlers.Processor processor = new ClientHandlers.Processor(myClientHandlersImpl);
            TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(myClientHandlersTransport)
                .processor(processor).executorService(new ThreadPoolExecutor(3, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue(), new ThreadFactory() {
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
            int cid = myRpc.connect(myThriftBackward);
            synchronized (myDelayedActions) {
                myIsConnected = true;
                myDelayedActions.forEach(r -> r.run());
                myDelayedActions.clear();
            }

            CefLog.Debug("Connected to CefSever, cid=" + cid);
        } catch (Throwable e) {
            CefLog.Error("RuntimeException in CefServer.connect: %s", e.getMessage());
            return false;
        } finally {
            synchronized (myDelayedActions) {
                myDelayedActions.clear();
            }
        }
        return true;
    }

    public void stop() {
        CefLog.Debug("Stop native server '%s'.", myThriftServer);
        myIsConnected = false;

        if (!DONT_STOP_SERVER_MANUALLY)
            myRpc.main.exec(r -> r.stop());
        myRpc.close();

        if (myClientHandlersTransport != null) {
            myClientHandlersTransport.close();
            myClientHandlersTransport = null;
        }
        if (myClientHandlersServer != null) {
            myClientHandlersServer.stop();
            myClientHandlersServer = null;
        }

        if (myThriftBackward != null)
            myThriftBackward.close();

        if (WAIT_FOR_SERVER_EXIT_SEC > 0) {
            CefLog.Debug("Waiting for server [%s] stop (max %d sec).", myThriftServer, WAIT_FOR_SERVER_EXIT_SEC);
            final long startMs = System.currentTimeMillis();
            final Thread t = new Thread(() -> {
                boolean stopped = NativeServerManager.waitForStopped(myThriftServer, WAIT_FOR_SERVER_EXIT_SEC*1000);
                if (stopped)
                    CefLog.Info("Server [%s] was stopped in %d ms.", myThriftServer, System.currentTimeMillis() - startMs);
                else
                    CefLog.Error("Can't stop server [%s] in %d seconds.", myThriftServer, WAIT_FOR_SERVER_EXIT_SEC);
            }, "CEF-shutdown-thread");
            t.setDaemon(false);
            t.start();
        }
    }

    public static TServer startTestHandlersService(CountDownLatch finished) {
        // Start dummy service for backward rpc calls (from native to java)
        TServerTransport transport;
        try {
            transport = ThriftTransport.ourDefaultClient.createServerTransport();
        } catch (Exception e) {
            CefLog.Error("Exception when opening test-client %s : %s", ThriftTransport.ourDefaultClient.isTcp() ? "tcp-socket" : "pipe", e.getMessage());
            if (ThriftTransport.ourDefaultClient.isTcp())
                CefLog.Error("Port : %d", ThriftTransport.ourDefaultClient.getPort());
            else
                CefLog.Error("Pipe : %s", ThriftTransport.ourDefaultClient.getPipe());
            return null;
        }

        ClientHandlers.Processor processor = new ClientHandlers.Processor(new ClientHandlersDummy());
        TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(transport)
                .processor(processor).executorService(new ThreadPoolExecutor(2, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue(), new ThreadFactory() {
                    final AtomicLong count = new AtomicLong();
                    public Thread newThread(Runnable r) {
                        final String name = String.format("CefHandlers(dummy)-execution-%d", this.count.getAndIncrement());
                        Thread thread = new Thread(r, name);
                        return thread;
                    }
                }));
        TServer result = new TThreadPoolServer(serverArgs) {
            @Override
            public void stop() {
                super.stop();
                transport.close();
            }
        };
        Thread t = new Thread(()-> {
            try {
                result.serve();
            } catch (Throwable e) {
                throw e;
            } finally {
                if (finished != null)
                    finished.countDown();
            }
        });
        t.setName("CefHandlers(dummy)-listening");
        t.start();
        return result;
    }
}
