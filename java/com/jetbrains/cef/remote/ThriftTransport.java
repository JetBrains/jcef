package com.jetbrains.cef.remote;

import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.cef.OS;
import org.cef.misc.CefLog;

import java.io.*;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

class ThriftTransport {
    protected static final int PORT = Integer.getInteger("jcef.remote.port", 9090);
    protected static final String PIPENAME = System.getProperty("jcef.remote.pipename", "client_pipe");

    static TServerTransport createServerTransport() throws IOException {
        if (OS.isWindows()) {
            WindowsPipeServerSocket pipeSocket = new WindowsPipeServerSocket(PIPENAME);
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

        final Path pipeName = Path.of(System.getProperty("java.io.tmpdir")).resolve("client_pipe");

        new File(pipeName.toString()).delete(); // cleanup file remaining from prev process

        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(UnixDomainSocketAddress.of(pipeName));

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
