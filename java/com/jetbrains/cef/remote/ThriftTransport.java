package com.jetbrains.cef.remote;

import org.apache.thrift.transport.*;
import org.cef.OS;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.*;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

class ThriftTransport {
    protected static final boolean USE_TCP = Utils.getBoolean("CEF_SERVER_USE_TCP");
    protected static final int PORT_CEF_SERVER = Utils.getInteger("ALT_CEF_SERVER_PORT", 9090);
    protected static final int PORT_JAVA_HANDLERS = Utils.getInteger("ALT_JAVA_HANDLERS_PORT", 9091);
    private static final String PIPENAME_JAVA_HANDLERS = Utils.getString("ALT_JAVA_HANDLERS_PIPE", "client_pipe");
    private static final String PIPENAME_CEF_SERVER = Utils.getString("ALT_CEF_SERVER_PIPE", "cef_server_pipe");

    static String getJavaHandlersPipe() {
        if (OS.isWindows())
            return PIPENAME_JAVA_HANDLERS;
        return Path.of(System.getProperty("java.io.tmpdir")).resolve(PIPENAME_JAVA_HANDLERS).toString();
    }

    static String getServerPipe() {
        if (OS.isWindows())
            return PIPENAME_CEF_SERVER;
        return Path.of(System.getProperty("java.io.tmpdir")).resolve(PIPENAME_CEF_SERVER).toString();
    }

    static TServerTransport createServerTransport() throws Exception {
        if (ThriftTransport.USE_TCP)
            return new TServerSocket(PORT_JAVA_HANDLERS);

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
}
