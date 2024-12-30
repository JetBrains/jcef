package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.browser.RemoteBrowser;
import com.jetbrains.cef.remote.browser.RemoteClient;
import com.jetbrains.cef.remote.thrift.transport.TTransportException;
import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import com.jetbrains.cef.remote.thrift.TException;
import com.jetbrains.cef.remote.thrift.server.TServer;
import com.jetbrains.cef.remote.thrift.server.TThreadPoolServer;
import com.jetbrains.cef.remote.thrift.transport.TServerTransport;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.handler.CefAppHandler;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CefServer {
    private static final CefServer INSTANCE = CefApp.isRemoteEnabled() ? new CefServer() : null;

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

    // Connects to CefServer and start cef-handlers service.
    // Should be executed in bg thread.
    // NOTE: appHandler is necessary for (1) cmdLineArgs, (2) custom schemes, (3) onContextInitialized callback
    public static boolean start(CefAppHandler appHandler, CefSettings settings) {
        if (!CefApp.isRemoteEnabled())
            return false;
        if (appHandler == null) { // just for simplicity
            CefLog.Error("Can't initialize client for native server because CefAppHandler is null.");
            return false;
        }

        final String root = NativeServerManager.isRunning();
        if (root != null) {
            // Shouldn't be here because pipe-names are unique for each client process.
            CefLog.Error("Found running cef_server instance with root '%s'", root);
        } else {
            List<Path> existingRoots = new ArrayList<>();
            if (!ThriftTransport.isTcp()) {
                File[] pipes = ThriftTransport.findPipes();
                if (pipes != null && pipes.length > 0) {
                    CefLog.Debug("Found %d pipes.", pipes.length);
                    for (File pipe: pipes) {
                        if (pipe.isFile()) {
                            RpcExecutor exec = new RpcExecutor();
                            try {
                                exec.openPipeTransport(ThriftTransport.getServerPipe().toString());
                                String newRoot = exec.execObj(s -> s.getServerInfo("root"));
                                existingRoots.add(Path.of(newRoot));
                                CefLog.Debug("Found new root_cache_path '%s' (pipe=%s).", newRoot, pipe.getName());
                                exec.closeTransport();
                            } catch (TTransportException e) {
                                CefLog.Debug("getServerInfo exception: %s", e.getMessage());
                            }
                        }
                    }
                }
            }

            if (!settings.cache_path.isEmpty()) {
                Path settingsRoot = Path.of(settings.cache_path);
                for (Path r: existingRoots)
                    if (r.equals(settingsRoot)) {
                        settings.cache_path = Path.of(System.getProperty("java.io.tmpdir")).resolve("cef_cache_" + ProcessHandle.current().pid()).toString();
                        CefLog.Info("The settings.cache_path='%s' conflicts with existing root_cache_path and will be replaced with '%s'.", r, settings.cache_path);
                        break;
                    }
            }

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

    public RpcContext getRpcContext() { return myRpc; }

    public RemoteClient createClient() {
        return new RemoteClient(myRpc, myBid2Browser);
    }

    public static String getVersion() {
        if (CefApp.isRemoteEnabled() && INSTANCE.myIsConnected)
            return INSTANCE.myRpc.main.execObj(r->r.getServerInfo("version"));
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
                myRpc.openTransport();
            } catch (TException x) {
                CefLog.Error("TException when opening server %s : %s", ThriftTransport.isTcp() ? "tcp-socket" : "pipe", x.getMessage());
                return false;
            }

            CefLog.Info("cef_server version: %s", (String)myRpc.main.execObj(r->r.getServerInfo("version")));

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
            int cid = myRpc.connect();
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

        myRpc.close();

        if (myClientHandlersTransport != null) {
            myClientHandlersTransport.close();
            myClientHandlersTransport = null;
        }
        if (myClientHandlersServer != null) {
            myClientHandlersServer.stop();
            myClientHandlersServer = null;
        }

        if (!OS.isWindows() && !ThriftTransport.isTcp())
            new File(ThriftTransport.getJavaHandlersPipe()).delete();
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
