package com.jetbrains.cef.remote;


import com.jetbrains.cef.remote.thrift_codegen.ClientHandlers;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.misc.CefLog;

import java.io.*;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class CefServer {
    private static final int PORT = Integer.getInteger("jcef.remote.port", 9090);
    private static final String PIPENAME = System.getProperty("jcef.remote.pipename", "client_pipe");
    private static final CefServer INSTANCE = CefApp.isRemoteEnabled() ? new CefServer() : null;

    // Fields for cef-handlers execution on java side
    private Thread myClientHandlersThread;
    private TServer myClientHandlersServer;
    private TServerTransport myClientHandlersTransport;
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
            myService.init("cef_server_pipe");

            // 2. Start service for backward rpc calls (from native to java)
            ClientHandlers.Processor processor = new ClientHandlers.Processor(myClientHandlersImpl);
            if (OS.isWindows()) {
                WindowsPipeServerSocket pipeSocket = new WindowsPipeServerSocket(PIPENAME);
                myClientHandlersTransport = new TServerTransport() {
                    @Override
                    public void listen() {}

                    @Override
                    public TTransport accept() throws TTransportException {
                        try {
                            Socket client = pipeSocket.accept();
                            return client != null ?
                                    new TIOStreamTransport(client.getInputStream(), client.getOutputStream()) : null;
                        } catch (IOException e) {
                            CefLog.Debug("Exception occurred during pipe listening: %s", e);
                            throw new TTransportException(TTransportException.UNKNOWN, e.getMessage());
                        }
                    }

                    @Override
                    public void close() {
                        try {
                            pipeSocket.close();
                        } catch (IOException e) {
                            CefLog.Error("Exception occurred during pipe closing: %s", e);
                        }
                    }
                };
            } else {
                // myClientHandlersTransport = new
                final Path pipeName = Path.of(System.getProperty("java.io.tmpdir")).resolve("client_pipe");

                new File(pipeName.toString()).delete(); // cleanup file remaining from prev process

                ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
                serverChannel.bind(UnixDomainSocketAddress.of(pipeName));

                myClientHandlersTransport = new TServerTransport() {
                    @Override
                    public void listen() {}

                    @Override
                    public TTransport accept() throws TTransportException {
                        try {
                            SocketChannel channel = serverChannel.accept();
                            InputStream is = new BufferedInputStream(Channels.newInputStream(channel));
                            OutputStream os = new BufferedOutputStream(Channels.newOutputStream(channel));
                            return new TIOStreamTransport(is, os);
                        } catch (IOException e) {
                            CefLog.Debug("Exception occurred during pipe listening: %s", e);
                            throw new TTransportException(TTransportException.UNKNOWN, e.getMessage());
                        }
                    }

                    @Override
                    public void close() {
                        try {
                            serverChannel.close();
                        } catch (IOException e) {
                            CefLog.Error("Exception occurred during pipe closing: %s", e);
                        }
                    }
                };
            }

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
                cid[0] = s.connect(PIPENAME, args, settings.toMap());
            });

            CefLog.Debug("Connected to CefSever, cid=" + cid[0]);
        } catch (TException x) {
            CefLog.Error("TException in CefServer.start: %s", x.getMessage());
            return false;
        } catch (IOException e) {
            CefLog.Error("IOException in CefServer.start: %s", e.getMessage());
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
