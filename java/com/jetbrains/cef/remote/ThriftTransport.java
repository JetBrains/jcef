package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift.transport.*;
import org.cef.OS;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class ThriftTransport {
    private static int PORT_CEF_SERVER = Utils.getInteger("ALT_CEF_SERVER_PORT", -1);
    private static int PORT_JAVA_HANDLERS = Utils.getInteger("ALT_JAVA_HANDLERS_PORT", -1);
    private static final String PIPENAME_JAVA_HANDLERS = Utils.getString("ALT_JAVA_HANDLERS_PIPE", "client_pipe");
    private static final String PIPENAME_CEF_SERVER = Utils.getString("ALT_CEF_SERVER_PIPE", "cef_server_pipe");
    private static final long PID = ProcessHandle.current().pid();
    private static final String SUFFIX = "_" + PID;
    private static final Path PIPE_DIR = Path.of(System.getProperty("java.io.tmpdir"));

    private final String myPipe;
    private final int myPort;

    public static final ThriftTransport ourDefaultServer;
    public static final ThriftTransport ourDefaultClient;

    static {
        ourDefaultServer = isTcpUsed() ? new ThriftTransport(getServerPort()) : new ThriftTransport(getServerPipe());
        ourDefaultClient = isTcpUsed() ? new ThriftTransport(getJavaHandlersPort()) : new ThriftTransport(getJavaHandlersPipe());
    }

    public ThriftTransport(String pipe) {
        this.myPipe = pipe;
        this.myPort = 0;
    }

    public ThriftTransport(int port) {
        this.myPipe = null;
        this.myPort = port;
    }

    public boolean isTcp() { return myPipe == null; }

    public String getPipe() { return myPipe; }
    public int getPort() { return myPort; }

    @Override
    public String toString() {
        return myPipe != null ? String.format("pipe='%s'", myPipe) : String.format("port=%d", myPort);
    }

    public void close() {
        if (!OS.isWindows() && !isTcp())
            new File(myPipe).delete();
    }

    private static String getJavaHandlersPipe() {
        if (OS.isWindows())
            return PIPENAME_JAVA_HANDLERS + SUFFIX;
        return PIPE_DIR.resolve(PIPENAME_JAVA_HANDLERS + SUFFIX).toString();
    }

    private static String getServerPipe() {
        if (OS.isWindows())
            return PIPENAME_CEF_SERVER + SUFFIX;
        return PIPE_DIR.resolve(PIPENAME_CEF_SERVER + SUFFIX).toString();
    }

    public static String getJavaHandlersPipe(String suffix) {
        if (OS.isWindows())
            return PIPENAME_JAVA_HANDLERS + "_" + suffix;
        return PIPE_DIR.resolve(PIPENAME_JAVA_HANDLERS + "_" + suffix).toString();
    }

    public static String getServerPipe(String suffix) {
        if (OS.isWindows())
            return PIPENAME_CEF_SERVER + "_" + suffix;
        return PIPE_DIR.resolve(PIPENAME_CEF_SERVER + "_" + suffix).toString();
    }

    public static boolean isTcpUsed() { return Utils.getBoolean("CEF_SERVER_USE_TCP"); }

    private static int getServerPort() {
        if (PORT_CEF_SERVER == -1) {
            PORT_CEF_SERVER = findFreePort();
            if (PORT_CEF_SERVER == -1)
                CefLog.Error("Can't find free tcp-port for server.");
            else
                CefLog.Info("Found free tcp-port %d for server.", PORT_CEF_SERVER);
        }
        return PORT_CEF_SERVER;
    }
    private static int getJavaHandlersPort() {
        if (PORT_JAVA_HANDLERS == -1) {
            PORT_JAVA_HANDLERS = findFreePort();
            if (PORT_JAVA_HANDLERS == -1)
                CefLog.Error("Can't find free tcp-port for java-handlers.");
            else
                CefLog.Info("Found free tcp-port %d for java-handlers.", PORT_JAVA_HANDLERS);
        }
        return PORT_JAVA_HANDLERS;
    }

    private static int findFreePort() { return findFreePort(6188, 7777); }

    private static int findFreePort(int from, int to) {
        for (int port = from; port < to; ++port) {
            try {
                ServerSocket ss = new ServerSocket(port);
                ss.close();
                return port;
            } catch (IOException e) {}
        }
        return -1;
    }

    public TServerTransport createServerTransport() throws Exception {
        if (isTcp())
            return new TServerSocket(myPort);

        if (OS.isWindows()) {
            WindowsPipeServerSocket pipeSocket = new WindowsPipeServerSocket(myPipe);
            return new TServerTransport() {
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
        }

        // Linux or OSX
        new File(myPipe).delete(); // cleanup file remaining from prev process

        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(UnixDomainSocketAddress.of(myPipe));

        return new TServerTransport() {
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

    public TIOStreamTransport openPipeTransport() throws TTransportException {
        try {
            InputStream is;
            OutputStream os;
            final Runnable closer;
            if (OS.isWindows()) {
                WindowsPipeSocket pipe = new WindowsPipeSocket(myPipe);
                is = pipe.getInputStream();
                os = pipe.getOutputStream();
                closer = ()->{
                    try {
                        pipe.close();
                    } catch (IOException e) {}
                };
            } else {
                SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(myPipe);
                channel.connect(socketAddress);
                is = Channels.newInputStream(channel);
                os = Channels.newOutputStream(channel);
                closer = ()->{
                    try {
                        channel.close();
                    } catch (IOException e) {}
                };
            }

            return new TIOStreamTransport(is, os) {
                @Override
                public void close() {
                    closer.run();
                }
            };
        } catch (IOException e) {
            throw new TTransportException(e.getMessage());
        }
    }

    public static File[] findPipes() {
        if (OS.isWindows()) {
            CefLog.Error("TODO: implement findPipes via Win32");
            return null;
        }

        return PIPE_DIR.toFile().listFiles((dir, name) -> name.startsWith(PIPENAME_CEF_SERVER));
    }
}
