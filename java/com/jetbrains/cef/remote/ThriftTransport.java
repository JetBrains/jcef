package com.jetbrains.cef.remote;

import org.apache.thrift.transport.*;
import org.cef.OS;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class ThriftTransport {
    private static int PORT_CEF_SERVER = Utils.getInteger("ALT_CEF_SERVER_PORT", -1);
    private static int PORT_JAVA_HANDLERS = Utils.getInteger("ALT_JAVA_HANDLERS_PORT", -1);
    private static final String PIPENAME_JAVA_HANDLERS = Utils.getString("ALT_JAVA_HANDLERS_PIPE", "client_pipe");
    private static final String PIPENAME_CEF_SERVER = Utils.getString("ALT_CEF_SERVER_PIPE", "cef_server_pipe");

    static String getJavaHandlersPipe() {
        if (OS.isWindows())
            return PIPENAME_JAVA_HANDLERS;
        return Path.of(System.getProperty("java.io.tmpdir")).resolve(PIPENAME_JAVA_HANDLERS).toString();
    }

    public static String getServerPipe() {
        if (OS.isWindows())
            return PIPENAME_CEF_SERVER;
        return Path.of(System.getProperty("java.io.tmpdir")).resolve(PIPENAME_CEF_SERVER).toString();
    }

    static boolean isTcp() { return Utils.getBoolean("CEF_SERVER_USE_TCP"); }
    static int getServerPort() {
        if (PORT_CEF_SERVER == -1) {
            PORT_CEF_SERVER = findFreePort();
            if (PORT_CEF_SERVER == -1)
                CefLog.Error("Can't find free tcp-port for server.");
            else
                CefLog.Info("Found free tcp-port %d for server.", PORT_CEF_SERVER);
        }
        return PORT_CEF_SERVER;
    }
    static int getJavaHandlersPort() {
        if (PORT_JAVA_HANDLERS == -1) {
            PORT_JAVA_HANDLERS = findFreePort();
            if (PORT_JAVA_HANDLERS == -1)
                CefLog.Error("Can't find free tcp-port for java-handlers.");
            else
                CefLog.Info("Found free tcp-port %d for java-handlers.", PORT_JAVA_HANDLERS);
        }
        return PORT_JAVA_HANDLERS;
    }

    static int findFreePort() { return findFreePort(6188, 7777); }

    static int findFreePort(int from, int to) {
        for (int port = from; port < to; ++port) {
            try {
                ServerSocket ss = new ServerSocket(port);
                ss.close();
                return port;
            } catch (IOException e) {}
        }
        return -1;
    }

    public static TServerTransport createServerTransport() throws Exception {
        if (isTcp())
            return new TServerSocket(getJavaHandlersPort());

        if (OS.isWindows()) {
            WindowsPipeServerSocket pipeSocket = new WindowsPipeServerSocket(getJavaHandlersPipe());
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

        final String pipePath = getJavaHandlersPipe();

        new File(pipePath).delete(); // cleanup file remaining from prev process

        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(UnixDomainSocketAddress.of(pipePath));

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

    public static TIOStreamTransport openPipeTransport(String pipeName) throws TTransportException {
        try {
            InputStream is;
            OutputStream os;
            final Runnable closer;
            if (OS.isWindows()) {
                WindowsPipeSocket pipe = new WindowsPipeSocket(pipeName);
                is = pipe.getInputStream();
                os = pipe.getOutputStream();
                closer = ()->{
                    try {
                        pipe.close();
                    } catch (IOException e) {}
                };
            } else {
                SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(pipeName);
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
}
