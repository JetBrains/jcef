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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CefServer {
    // NOTE: TeamCity runs tests in parallel with downloading (and other processes), and because of that
    // first CEF initialization takes a long time (more than 15 sec in 1% of test runs). So use a large constant here.
    private static final int WAIT_FOR_SERVER_START_SEC = Utils.getInteger("JCEF_WAIT_FOR_SERVER_START_SEC", 60);
    private static final int WAIT_FOR_SERVER_EXIT_SEC = Utils.getInteger("JCEF_WAIT_FOR_SERVER_EXIT_SEC", 10);
    private static final boolean DONT_STOP_SERVER_MANUALLY = Utils.getBoolean("JCEF_DONT_STOP_SERVER_MANUALLY"); // TODO: remove after platform tests debugging
    private static final boolean DONT_USE_UNIQUE_ROOTS = Utils.getBoolean("JCEF_DONT_USE_UNIQUE_ROOTS"); // TODO: remove after platform tests debugging

    private static Map<CefParams, CefServer> ourInstances = new ConcurrentHashMap<>();

    private final ThriftTransport myThriftServer;
    private final ThriftTransport myThriftBackward;
    private final String[] myArgs;
    private final CefSettings mySettings;

    private CefApp myCefApp = null;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerTransport myClientHandlersTransport;
    private final RpcContext myRpc;
    private final Map<Integer, RemoteBrowser> myBid2Browser = new ConcurrentHashMap<>();
    private final ClientHandlersImpl myClientHandlersImpl;

    private volatile boolean myIsConnected = false;
    private volatile boolean myIsContextInitialized = false;

    private final LinkedList<Runnable> myDelayedActions = new LinkedList<>();

    public CefServer(ThriftTransport thriftServer, ThriftTransport thriftBackward, String[] args, CefSettings settings) {
        myThriftServer = thriftServer;
        myThriftBackward = thriftBackward;
        myArgs = args == null ? new String[0] : Arrays.copyOf(args, args.length);
        mySettings = settings == null ? new CefSettings() : settings.clone();

        myRpc = new RpcContext(this);
        myClientHandlersImpl = new ClientHandlersImpl(myRpc, myBid2Browser);

        CefParams p = new CefParams(mySettings, myArgs);
        if (ourInstances.get(p) != null)
            CefLog.Error("CefServer instance already created for params:\n%s", p);
        ourInstances.put(p, this);
        CefLog.Debug("Created CefServer instance. Transport %s (backward %s). Params:\n%s", thriftServer, thriftBackward, p);
    }

    public static CefServer findInstance(String[] args, CefSettings settings) {
        return ourInstances.get(new CefParams(settings, args));
    }

    public static int getInstancesCount() { return ourInstances.size(); }

    public ThriftTransport getThriftServer() { return myThriftServer; }
    public ThriftTransport getThriftBackward() { return myThriftBackward; }

    public CefApp getCefApp() { return myCefApp; }
    public void setCefApp(CefApp cefApp) { myCefApp = cefApp; }

    public boolean start(CefAppHandler appHandler) {
        return start(appHandler, true);
    }

    // Connects to CefServer and start cef-handlers service.
    // Should be executed in bg thread.
    // NOTE: appHandler is necessary for (1) custom schemes, (2) onContextInitialized callback
    public boolean start(CefAppHandler appHandler, boolean fixRoot) {
        if (!CefApp.isRemoteEnabled())
            return false;

        final String prevRoot = NativeServerManager.isRunning(myThriftServer);
        if (prevRoot != null) {
            // Shouldn't be here because pipe-names are unique for each client process.
            CefLog.Error("Found running cef_server instance with root '%s'", prevRoot);
        } else {
            boolean deleteRoot = false;
            if (fixRoot && !DONT_USE_UNIQUE_ROOTS)
                deleteRoot = NativeServerManager.fixRootInSettings(mySettings, "cef_cache_" + myThriftServer.toStringShort());

            final boolean success = NativeServerManager.startProcessAndWait(myThriftServer, appHandler, myArgs, mySettings, deleteRoot, WAIT_FOR_SERVER_START_SEC*1000l);
            if (!success)
                return false;
        }

        if (!connect(appHandler == null ? null : appHandler::onContextInitialized)) {
            CefLog.Error("Can't initialize thrift client for native server.");
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

    public String getVersion() {
        if (myIsConnected)
            return myRpc.execObj(r->r.getServerInfo("version"));
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

            CefLog.Info("cef_server version: %s", (String)myRpc.execObj(r->r.getServerInfo("version")));

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
            myRpc.exec(r -> r.stop());
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

        ourInstances.remove(new CefParams(mySettings, myArgs));
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

    //
    // Convenience methods
    //
    public void exec(RpcExecutor.Rpc r) {
        myRpc.exec(r);
    }

    public <T> T execObj(RpcExecutor.RpcObj<T> r) {
        return myRpc.execObj(r);
    }

    public static class CefParams {
        final Set<String> chromiumArgs = new HashSet<>();
        final CefSettings settings;

        public CefParams(CefSettings settings, String[] args) {
            this.settings = settings == null ? new CefSettings() : settings.clone();
            if (args != null && args.length > 0) {
                for (String arg: args)
                    if (arg != null)
                        chromiumArgs.add(arg.trim());
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CefParams) {
                final CefParams cp = (CefParams)obj;
                if (!chromiumArgs.equals(cp.chromiumArgs))
                    return false;
                return Objects.equals(settings.log_file, cp.settings.log_file)
                       && Objects.equals(settings.log_severity, cp.settings.log_severity);
                // TODO: add another significant fields from CefSettings.
                // Candidates: javascript_flags, remote_debugging_port, etc
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int argsHash = chromiumArgs.hashCode();
            if (settings == null)
                return argsHash;

            final int f = settings.log_file == null ? 0 : settings.log_file.hashCode();
            final int l = settings.log_severity == null ? 0 : settings.log_severity.hashCode();
            return f + l + argsHash;
        }

        @Override
        public String toString() {
            return chromiumArgs + "| log_file=" + settings.log_file + ", log_severity=" + settings.log_severity;
        }
    }
}
