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

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CefServer {
    private static final boolean CONNECT_AS_SLAVE = Utils.getBoolean("JCEF_CONNECT_AS_SLAVE");

    private static final CefServer INSTANCE = CefApp.isRemoteEnabled() ? new CefServer() : null;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerTransport myClientHandlersTransport;
    private final RpcExecutor myRpc = new RpcExecutor();
    private final Map<Integer, RemoteBrowser> myBid2Browser = new ConcurrentHashMap<>();
    private final ClientHandlersImpl myClientHandlersImpl = new ClientHandlersImpl(myRpc, myBid2Browser);

    private volatile boolean myIsConnected = false;
    private volatile boolean myIsContextInitialized = false;

    private final LinkedList<Runnable> myDelayedActions = new LinkedList<>();

    // Connects to CefServer and start cef-handlers service.
    // Should be executed in bg thread.
    public static boolean connect(CefAppHandler appHandler, CefSettings settings) {
        if (!CefApp.isRemoteEnabled())
            return false;

        if (NativeServerManager.isRunning()) {
            CefLog.Error("Found running cef_server instance. TODO: we must check that running instance has the same <CefSettings, cmd-line switches, custom schemes> and restart cef_server with correct args if necessary.");
        } else {
            final long waitTimeoutMs = Utils.getInteger("WAIT_SERVER_TIMEOUT_MS", 15000);
            final boolean success = NativeServerManager.startProcessAndWait(appHandler, settings, waitTimeoutMs);
            if (!success)
                return false;
        }

        if (!INSTANCE.connect(appHandler::onContextInitialized)) {
            CefLog.Error("Can't initialize client for native server.");
            return false;
        }
        return true;
    }

    public static CefServer instance() { return INSTANCE; }

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

    public RpcExecutor getService() { return myRpc; }

    public RemoteClient createClient() {
        return new RemoteClient(myRpc, myBid2Browser);
    }

    public static String getVersion() {
        if (CefApp.isRemoteEnabled() && INSTANCE.myIsConnected)
            return INSTANCE.myRpc.execObj(r->r.version());
        return "unknown(not connected)";
    }

    private boolean connect(Runnable onContextInitialized) {
        myClientHandlersImpl.setOnContextInitialized(() -> {
            myIsContextInitialized = true;
            onContextInitialized.run();
        });

        try {
            // 1. Start server for cef-handlers execution. Open transport for rpc-handlers
            try {
                CefLog.Debug("Initialize CefServer, open server transport.");
                myRpc.openTransport();
            } catch (TException x) {
                CefLog.Error("TException when opening server %s : %s", ThriftTransport.isTcp() ? "tcp-socket" : "pipe", x.getMessage());
                return false;
            }

            // 2. Start service for backward rpc calls (from native to java)
            try {
                myClientHandlersTransport = ThriftTransport.createServerTransport();
            } catch (Exception e) {
                CefLog.Error("Exception when opening client %s : %s", ThriftTransport.isTcp() ? "tcp-socket" : "pipe", e.getMessage());
                if (ThriftTransport.isTcp())
                    CefLog.Error("Port : %d", ThriftTransport.getJavaHandlersPort());
                else
                    CefLog.Error("Pipe : %s", ThriftTransport.getJavaHandlersPipe());
                return false;
            }

            ClientHandlers.Processor processor = new ClientHandlers.Processor(myClientHandlersImpl);
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
            int cid = myRpc.connect(!CONNECT_AS_SLAVE);
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

    public void disconnect() {
        CefLog.Debug("Disconnect from native server (it will be automatically stopped soon because we were connected as master).");
        myIsConnected = false;

        myRpc.closeTransport();

        if (myClientHandlersTransport != null) {
            myClientHandlersTransport.close();
            myClientHandlersTransport = null;
        }
        if (myClientHandlersServer != null) {
            myClientHandlersServer.stop();
            myClientHandlersServer = null;
        }
    }

    public static TServer startTestHandlersService(CountDownLatch finished) {
        // Start dummy service for backward rpc calls (from native to java)
        TServerTransport transport;
        try {
            transport = ThriftTransport.createServerTransport();
        } catch (Exception e) {
            CefLog.Error("Exception when opening test-client %s : %s", ThriftTransport.isTcp() ? "tcp-socket" : "pipe", e.getMessage());
            if (ThriftTransport.isTcp())
                CefLog.Error("Port : %d", ThriftTransport.getJavaHandlersPort());
            else
                CefLog.Error("Pipe : %s", ThriftTransport.getJavaHandlersPipe());
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
