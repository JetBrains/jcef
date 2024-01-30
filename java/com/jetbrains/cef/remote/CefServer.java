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

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CefServer {
    private static final CefServer INSTANCE = CefApp.isRemoteEnabled() ? new CefServer() : null;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerTransport myClientHandlersTransport;
    private final RpcExecutor myService = new RpcExecutor();
    private final ClientHandlersImpl myClientHandlersImpl = new ClientHandlersImpl(myService);
    private volatile boolean myIsInitialized = false;

    // Connects to CefServer and start cef-handlers service.
    // Should be executed in bg thread.
    public static boolean initialize(CefAppHandler appHandler, CefSettings settings) {
        if (!CefApp.isRemoteEnabled())
            return false;

        if (!NativeServerManager.startIfNecessary(appHandler, settings))
            return false;

        if (!INSTANCE.initialize(appHandler)) {
            CefLog.Error("Can't initialize client for native server.");
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

    private boolean initialize(CefAppHandler appHandler) {
        try {
            myClientHandlersImpl.setAppHandler(appHandler);

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
                        Thread thread = new Thread(r);
                        thread.setDaemon(true);
                        thread.setName(String.format("CefHandlers-execution-%d", this.count.getAndIncrement()));
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

    public void stop() {
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
