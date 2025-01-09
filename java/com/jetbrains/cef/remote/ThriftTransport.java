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
import java.util.HashSet;
import java.util.Set;

public class ThriftTransport {
    private static final boolean IS_TCP_USED;
    private static final int PORT_CEF_SERVER;
    private static final int PORT_JAVA_HANDLERS;
    private static final String PIPENAME_JAVA_HANDLERS;
    private static final String PIPENAME_CEF_SERVER;
    private static final long PID = ProcessHandle.current().pid();
    private static final String SUFFIX = "_" + PID;
    private static final Path PIPE_DIR = Path.of(System.getProperty("java.io.tmpdir"));

    private final String myPipe;
    private final int myPort;

    public static final ThriftTransport ourDefaultServer;
    public static final ThriftTransport ourDefaultClient;

    static {
        if (OS.isWindows()) {
            IS_TCP_USED = !Utils.getBoolean("CEF_SERVER_USE_PIPE");
        } else {
            IS_TCP_USED = Utils.getBoolean("CEF_SERVER_USE_TCP");
        }

        if (IS_TCP_USED) {
            int customPort = Utils.getInteger("ALT_CEF_SERVER_PORT", -1);
            if (customPort == -1) {
                PORT_CEF_SERVER = findFreePort(null);
                if (PORT_CEF_SERVER == -1)
                    CefLog.Error("Can't find free tcp-port for server.");
                else
                    CefLog.Info("Found free tcp-port %d for server.", PORT_CEF_SERVER);
            } else {
                CefLog.Info("Use custom tcp-port %d for server.", customPort);
                PORT_CEF_SERVER = customPort;
            }

            customPort = Utils.getInteger("ALT_JAVA_HANDLERS_PORT", -1);
            if (customPort == -1) {
                Set<Integer> exclude = new HashSet<>(); exclude.add(PORT_CEF_SERVER);
                PORT_JAVA_HANDLERS = findFreePort(exclude);
                if (PORT_JAVA_HANDLERS == -1)
                    CefLog.Error("Can't find free tcp-port for java-handlers.");
                else
                    CefLog.Info("Found free tcp-port %d for java-handlers.", PORT_JAVA_HANDLERS);
            } else {
                CefLog.Info("Use custom tcp-port %d for java-handlers.", customPort);
                PORT_JAVA_HANDLERS = customPort;
            }

            ourDefaultServer = new ThriftTransport(getServerPort());
            ourDefaultClient = new ThriftTransport(getJavaHandlersPort());

            PIPENAME_JAVA_HANDLERS = "";
            PIPENAME_CEF_SERVER = "";
        } else {
            PORT_CEF_SERVER = 0;
            PORT_JAVA_HANDLERS = 0;

            final String pipeServerDefault = "cef_server_pipe";
            final String pipeServerCustom = Utils.getString("ALT_CEF_SERVER_PIPE");
            final String suffixServer;
            if (pipeServerCustom == null || pipeServerCustom.isEmpty()) {
                PIPENAME_CEF_SERVER = pipeServerDefault;
                suffixServer = SUFFIX;
            } else {
                PIPENAME_CEF_SERVER = pipeServerCustom;
                suffixServer = "";
            }

            String pipeJavaDefault = "client_pipe";
            String pipeJavaCustom = Utils.getString("ALT_JAVA_HANDLERS_PIPE");
            final String suffixJava;
            if (pipeJavaCustom == null || pipeJavaCustom.isEmpty()) {
                PIPENAME_JAVA_HANDLERS = pipeJavaDefault;
                suffixJava = SUFFIX;
            } else {
                PIPENAME_JAVA_HANDLERS = pipeJavaCustom;
                suffixJava = "";
            }

            String pipe;
            if (OS.isWindows())
                pipe = PIPENAME_CEF_SERVER + suffixServer;
            else
                pipe = PIPE_DIR.resolve(PIPENAME_CEF_SERVER + suffixServer).toString();
            ourDefaultServer = new ThriftTransport(pipe);

            if (OS.isWindows())
                pipe = PIPENAME_JAVA_HANDLERS + suffixJava;
            else
                pipe = PIPE_DIR.resolve(PIPENAME_JAVA_HANDLERS + suffixJava).toString();
            ourDefaultClient = new ThriftTransport(pipe);
        }
    }

    public ThriftTransport(File pipe) {
        this.myPipe = OS.isWindows() ? pipe.getName() : pipe.getAbsolutePath() ;
        this.myPort = 0;
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

    public static boolean isTcpUsed() { return IS_TCP_USED; }

    private static int getServerPort() {

        return PORT_CEF_SERVER;
    }
    private static int getJavaHandlersPort() {
        return PORT_JAVA_HANDLERS;
    }

    public static int findFreePort(Set<Integer> exclude) { return findFreePort(6188, 7777, exclude); }

    public static int findFreePort(int from, int to, Set<Integer> exclude) {
        for (int port = from; port < to; ++port) {
            if (exclude != null && exclude.contains(port))
                continue;
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
            String[] pipes = WindowsPipe.findPipes(PIPENAME_CEF_SERVER + "*");
            if (pipes == null || pipes.length == 0)
                return null;
            File[] result = new File[pipes.length];
            for (int i = 0; i < pipes.length; i++)
                result[i] = new File(pipes[i]);
            return result;
        }

        return PIPE_DIR.toFile().listFiles((dir, name) -> name.startsWith(PIPENAME_CEF_SERVER));
    }
}
