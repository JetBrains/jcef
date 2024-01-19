package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.Server;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.cef.OS;
import org.cef.misc.CefLog;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class RpcExecutor {
    private TTransport myTransport;
    private TProtocol myProtocol;
    private Server.Iface myServer;

    public RpcExecutor() {}
    public RpcExecutor(String pipeName) throws TTransportException { init(pipeName); }

    public void init(int port) throws TTransportException {
        try {
            myTransport = new TSocket("localhost", port);
            myTransport.open();
            myProtocol = new TBinaryProtocol(myTransport);
            myServer = new Server.Client(myProtocol);
        } catch (TTransportException e) {
            myTransport = null;
            throw e;
        }
    }

    public void init(String pipeName) throws TTransportException {
        try {
            myTransport = openPipeTransport(pipeName);
            myProtocol = new TBinaryProtocol(myTransport);
            myServer = new Server.Client(myProtocol);
        } catch (TTransportException e) {
            myTransport = null;
            throw e;
        }
    }

    public static TIOStreamTransport openPipeTransport(String pipeName) throws TTransportException {
        try {
            InputStream is;
            OutputStream os;
            if (OS.isWindows()) {
                WindowsPipeSocket pipe = new WindowsPipeSocket(pipeName);
                is = pipe.getInputStream();
                os = pipe.getOutputStream();
            } else {
                final Path pipePath = Path.of(System.getProperty("java.io.tmpdir")).resolve(pipeName);
                SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(pipePath);
                channel.connect(socketAddress);
                is = Channels.newInputStream(channel);
                os = Channels.newOutputStream(channel);
            }
            // TODO: should we use new BufferedInputStream(is/os) ?

            return new TIOStreamTransport(is, os);
        } catch (IOException e) {
            throw new TTransportException(e.getMessage());
        }
    }

    public interface Rpc {
        void run(Server.Iface s) throws TException;
    }
    public interface RpcObj<T>  {
        T run(Server.Iface s) throws TException;
    }

    public TTransport getTransport() {
        return myTransport;
    }

    synchronized
    public void exec(Rpc r) {
        if (myTransport == null)
            return;
        try {
            r.run(myServer);
        } catch (TException e) {
            onThriftException(e);
        }
    }

    synchronized
    public <T> T execObj(RpcObj<T> r) {
        if (myTransport == null)
            return null;
        try {
            return r.run(myServer);
        } catch (TException e) {
            onThriftException(e);
        }
        return null;
    }

    synchronized
    void closeTransport() {
        if (myTransport != null) {
            myTransport.close();
            myTransport = null;
        }
    }

    private void onThriftException(TException e) {
        CefLog.Error("thrift exception '%s'", e.getMessage());
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        CefLog.Error(sw.getBuffer().toString());
    }
}
