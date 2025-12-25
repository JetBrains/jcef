package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift.transport.TTransportException;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.util.concurrent.LinkedBlockingQueue;

public class RpcContext {
    private static final boolean REWIND_QUEUE = Utils.getBoolean("JCEF_REWIND_QUEUE"); // temporary key for advanced teamcity tests
    private static final RpcExecutor.Rpc NO_RPC = s -> {};
    public final CefServer server;
    private final RpcExecutor myMain;
    private final RpcExecutor myBackground;
    private final LinkedBlockingQueue<RpcExecutor.Rpc> myQueue = new LinkedBlockingQueue();
    private final Thread myThread;
    private volatile boolean myClosed = false;

    public RpcContext(CefServer server) {
        this.server = server;
        myMain = new RpcExecutor(server);
        myBackground = new RpcExecutor(server);
        myThread = new Thread(() -> {
            while (true) {
                try {
                    RpcExecutor.Rpc rpc = myQueue.take();
                    if (rpc == NO_RPC)
                        return;
                    myBackground.exec(rpc);
                } catch (InterruptedException e) {
                    CefLog.Warn("RpcContext bg thread interrupted: %s", e.getMessage());
                    return;
                }
            }
        });
        myThread.setName("CefBgThread");
        myThread.setDaemon(true);
        myThread.start();
    }

    public void openTransport(ThriftTransport thriftServer) throws TTransportException {
        myMain.openTransport(thriftServer);
        myBackground.openTransport(thriftServer);
    }

    public int connect(ThriftTransport thriftBackward, boolean asMaster) {
        int cid = myMain.connect(thriftBackward, asMaster);
        myBackground.exec(s -> s.attach(cid));
        return cid;
    }

    public void invokeLater(RpcExecutor.Rpc rpc) {
        myQueue.add(rpc);
    }

    public void close() {
        if (myClosed)
            return;
        myClosed = true;
        myQueue.add(NO_RPC); // It will cause bg-thread to stop.
        if (REWIND_QUEUE) {
            try {
                myThread.join(); // wait for all queued rpc
            } catch (InterruptedException e) {
                CefLog.Debug("RpcContext join interrupted: %s", e.getMessage());
            }
        }
        myMain.closeTransport();
        myBackground.closeTransport();
    }

    //
    // Convenience methods
    //
    public void exec(RpcExecutor.Rpc r) {
        myMain.exec(r);
    }

    public <T> T execObj(RpcExecutor.RpcObj<T> r) {
        return myMain.execObj(r);
    }
}
