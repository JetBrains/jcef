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

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CefServer {
    // NOTE: TeamCity runs tests in parallel with downloading (and other processes), and because of that
    // first CEF initialization takes a long time (more than 15 sec in 1% of test runs). So use a large constant here.
    private static final int WAIT_FOR_SERVER_START_SEC = Utils.getInteger("JCEF_WAIT_FOR_SERVER_START_SEC", 60);
    private static final int WAIT_FOR_SERVER_EXIT_SEC = Utils.getInteger("JCEF_WAIT_FOR_SERVER_EXIT_SEC", 10);

    private static HashSet<CefServer> ourInstances = new HashSet<>();

    private final ThriftTransport myThriftServer;
    private final File myServerExe;
    private final boolean myConnectAsMaster;
    private ThriftTransport myThriftBackward;
    private final CefParams myParams;

    private CefApp myCefApp = null;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerTransport myClientHandlersTransport;
    private final RpcContext myRpc;
    private final ClientHandlersImpl myClientHandlersImpl;

    private volatile boolean myIsConnected = false;
    private volatile boolean myIsContextInitialized = false;
    private volatile boolean myIsDisconnected = false;
    private volatile boolean myIsCrashed = false;

    private final LinkedList<Runnable> myDelayedActions = new LinkedList<>();

    private Runnable myDisconnectionCallback = null;

    public final Map<Integer, RemoteClient> cid2Client = new ConcurrentHashMap<>();
    public final Map<Integer, RemoteBrowser> bid2Browser = new ConcurrentHashMap<>();

    public CefServer(ThriftTransport transport, String[] args, CefSettings settings) {
        this(NativeServerManager.getServerExe(), transport, args, settings);
    }
    public CefServer(File serverExe, ThriftTransport transport, String[] args, CefSettings settings) {
        this(serverExe, transport, args, settings, true);
    }
    public CefServer(File serverExe, ThriftTransport transport, String[] args, CefSettings settings, boolean connectAsMaster) {
        myThriftServer = transport;
        myServerExe = serverExe;
        myConnectAsMaster = connectAsMaster;
        myParams = new CefParams(settings, args);

        myRpc = new RpcContext(this);
        myClientHandlersImpl = new ClientHandlersImpl(myRpc);

        CefServer otherRunningInstance = findInstance(myParams, true);
        if (otherRunningInstance != null)
            CefLog.Error("Found running CefServer instance for params:\n%s", myParams);
        ourInstances.add(this);

        CefLog.Debug("Created CefServer instance '%s' with transport '%s'. Params:\n%s", toStringShort(), transport, myParams);
    }

    public static CefServer findInstance(CefParams params, boolean onlyRunning) {
        if (params == null)
            return null;

        CefLog.Debug("Find for params: " + params);
        synchronized (ourInstances) {
            for (CefServer s: ourInstances) {
                if (onlyRunning) {
                    if (s.myIsDisconnected)
                        continue;
                    final CefApp app = s.getCefApp();
                    if (app != null && app.isShuttingDown())
                        continue;
                }
                if (s.myParams.isAlmostEqual(params))
                    return s;
            }
            return null;
        }
    }

    public static CefServer findRunningInstance(String[] args, CefSettings settings) {
        return findInstance(new CefParams(settings, args), true);
    }

    public static int getInstancesCount() {
        synchronized (ourInstances) {
            return ourInstances.size();
        }
    }

    public static void logInstances() {
        if (CefLog.IsDebugEnabled()) {
            CefLog.Debug("Available CefServer instances: ");
            synchronized (ourInstances) {
                for (CefServer s : ourInstances)
                    CefLog.Debug(s.toStringDetailed());
            }
        }
    }

    public ThriftTransport getThriftServer() { return myThriftServer; }

    public String[] getArgs() { return myParams.args; }
    public CefSettings getCefSettings() { return myParams.settings; }

    public String toStringShort() { return myThriftServer.toStringShort(); }
    public String toStringDetailed() { return "CefServer_" + myThriftServer.toStringShort() + ", params: " + myParams.toString(); }

    public CefApp getCefApp() { return myCefApp; }
    public void setCefApp(CefApp cefApp) { myCefApp = cefApp; }

    public void setDisconnectionCallback(Runnable disconnectionCallback) { myDisconnectionCallback = disconnectionCallback; }

    // Connects to CefServer and start cef-handlers service.
    // Should be executed in bg thread.
    // NOTE: appHandler is necessary for (1) custom schemes, (2) onContextInitialized callback
    public boolean start(CefAppHandler appHandler) {
        try {
            if (!CefApp.isRemoteEnabled())
                return false;

            final String runningRoot = NativeServerManager.isRunning(myThriftServer);
            if (runningRoot != null) {
                // NOTE: pipe-names/ports are unique for each client process, so we can go here only when custom transport is specified manually.
                CefLog.Info("Going to connect with already running cef_server: transport '%s', root '%s'", myThriftServer, runningRoot);
            } else {
                final boolean success = ServerStarter.startProcessAndWait(myServerExe, myThriftServer, appHandler, myParams.args, myParams.settings, false, null, WAIT_FOR_SERVER_START_SEC*1000l);
                if (!success)
                    return false;
            }

            if (!connect(appHandler == null ? null : appHandler::onContextInitialized)) {
                CefLog.Error("CefServer.connect() fails, can't initialize thrift client for native server.");
                return false;
            }
            return true;
        }  catch (Throwable e) {
            CefLog.Error("RuntimeException in CefServer.start: %s", e.getMessage());
            return false;
        } finally {
            synchronized (myDelayedActions) {
                myDelayedActions.clear();
            }
        }
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
        return new RemoteClient(myRpc);
    }

    public String getVersion() {
        if (myIsConnected)
            return myRpc.execObj(r->r.getServerInfo("version"));
        return "unknown(not connected)";
    }

    public String getExePath() {
        if (myServerExe != null)
            return myServerExe.getAbsolutePath();
        final File f = NativeServerManager.getServerExe();
        return f != null ? f.getAbsolutePath() : null;
    }

    private boolean connect(Runnable onContextInitialized) {
        myClientHandlersImpl.setOnContextInitialized(() -> {
            myIsContextInitialized = true;
            if (onContextInitialized != null)
                onContextInitialized.run();
        });

        int cid = -1;
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

            // 2. Open transport for backward rpc calls (from native to java)
            if (ThriftTransport.isTcpUsed())
                myThriftBackward = new ThriftTransport(ThriftTransport.findFreePort(null));
            else {
                // Use unique suffix for pipe name to avoid filename collisions.
                final String suffix = "" + System.currentTimeMillis();
                myThriftBackward = new ThriftTransport(ThriftTransport.getJavaHandlersPipe(suffix));
            }
            try {
                myClientHandlersTransport = myThriftBackward.createServerTransport();
            } catch (Exception e) {
                CefLog.Error("Exception when opening backward transport '%s'", myThriftBackward.toString(), e.getMessage());
                return false;
            }

            // 3. Start service for backward rpc calls (from native to java)
            ClientHandlers.Processor processor = new ClientHandlers.Processor(myClientHandlersImpl);
            TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(myClientHandlersTransport)
                .processor(processor).executorService(new ThreadPoolExecutor(3, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue(), new ThreadFactory() {
                    final AtomicLong count = new AtomicLong();
                    public Thread newThread(Runnable r) {
                        final String name = String.format("CefHandlers-execution-%d", this.count.getAndIncrement());
                        Thread thread = new Thread(()->{
                            r.run();
                            CefServer.this.onCefHandlersThreadFinished();
                        }, name);
                        thread.setDaemon(true);
                        return thread;
                    }
                }));
            myClientHandlersServer = new TThreadPoolServer(serverArgs);
            myClientHandlersThread = new Thread(()-> myClientHandlersServer.serve());
            myClientHandlersThread.setName("CefHandlers-listening");
            myClientHandlersThread.start();

            // 4. Connect to CefServer.
            cid = myRpc.connect(myThriftBackward, myConnectAsMaster);
            if (cid == -1) {
                CefLog.Error("Can't connect to '%s', cid==-1", this.toStringDetailed());
                return false;
            }

            CefLog.Debug("Connected to '%s', cid=%d", this.toStringDetailed(), cid);
        } catch (Throwable e) {
            CefLog.Error("RuntimeException in CefServer.connect: %s", e.getMessage());
            return false;
        } finally {
            synchronized (myDelayedActions) {
                if (cid != -1) {
                    myIsConnected = true;
                    myDelayedActions.forEach(r -> r.run());
                }
                myDelayedActions.clear();
            }
        }
        return true;
    }

    public void stop() {
        CefLog.Debug("Disconnect from native server '%s'.", myThriftServer);

        disconnect();

        if (WAIT_FOR_SERVER_EXIT_SEC > 0) {
            if (myIsCrashed)
                CefLog.Debug("Server [%s] was crashed, will skip waiting for stop.", myThriftServer);
            else {
                CefLog.Debug("Waiting for server [%s] stop (max %d sec).", myThriftServer, WAIT_FOR_SERVER_EXIT_SEC);
                final long startMs = System.currentTimeMillis();
                final Thread t = new Thread(() -> {
                    boolean stopped = NativeServerManager.waitForStopped(myThriftServer, WAIT_FOR_SERVER_EXIT_SEC * 1000);
                    if (stopped)
                        CefLog.Info("Server [%s] was stopped in %d ms.", myThriftServer, System.currentTimeMillis() - startMs);
                    else
                        CefLog.Error("Can't stop server [%s] in %d seconds.", myThriftServer, WAIT_FOR_SERVER_EXIT_SEC);
                }, "CEF-shutdown-thread");
                t.setDaemon(false);
                t.start();
            }
        }
        synchronized (ourInstances) {
            ourInstances.remove(this);
        }
    }

    void onRpcThriftException(TException e) {
        onDisconnected(e);
    }

    void onCefHandlersThreadFinished() {
        onDisconnected(null);
    }

    private void disconnect() {
        myIsConnected = false;
        myIsDisconnected = true;
        myRpc.close();

        if (myClientHandlersServer != null) {
            myClientHandlersServer.stop();
            myClientHandlersServer = null;
        }
        if (myClientHandlersTransport != null) {
            myClientHandlersTransport.close();
            myClientHandlersTransport = null;
        }
    }

    private void onDisconnected(TException e) {
        if (!myIsConnected)
            return;

        //
        // Check CefApp state and notify the user if necessary.
        //
        final CefApp app = myCefApp;
        if (app == null) {
            if (e == null)
                CefLog.Error("CefApp of server '%s' is null when disconnection happened.", toStringShort());
            else
                CefLog.Error("CefApp of server '%s' is null, thrift exception: %s", toStringShort(), e.getMessage());
        } else if (!app.isShuttingDown()) {
            myIsCrashed = true;
            if (e == null)
                CefLog.Error("CefApp of server '%s' isn't shutting down, but java-client is disconnected", toStringShort());
            else
                CefLog.Error("CefApp of server '%s' isn't shutting down, but java-client is disconnected, thrift exception: %s", toStringShort(), e.getMessage());
            if (myDisconnectionCallback != null)
                myDisconnectionCallback.run();
        }

        //
        // Set the 'isConnected' flag and perform close actions.
        //
        disconnect();
    }

    public static TServer startTestHandlersService(ThriftTransport backward, CountDownLatch finished) {
        // Start dummy service for backward rpc calls (from native to java)
        TServerTransport transport;
        try {
            transport = backward.createServerTransport();
        } catch (Exception e) {
            CefLog.Error("Exception when opening transport '%s' for test-client, error: %s", backward.toString(), e.getMessage());
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
        final String[] args;
        final Set<String> argsSet = new HashSet<>();
        final CefSettings settings;

        public CefParams(CefSettings settings, String[] args) {
            this.args = args == null ? new String[0] : Arrays.copyOf(args, args.length);
            this.settings = settings == null ? new CefSettings() : settings.clone();
            if (args != null && args.length > 0) {
                for (String arg: args)
                    if (arg != null)
                        argsSet.add(arg.trim());
            }
        }
        
        public boolean isAlmostEqual(CefParams cp) {
            if (!argsSet.equals(cp.argsSet))
                return false;
            return settings.isAlmostEqual(cp.settings);
        }

        @Override
        public String toString() {
            return argsSet + "| " + settings.getDescription();
        }
    }
}
