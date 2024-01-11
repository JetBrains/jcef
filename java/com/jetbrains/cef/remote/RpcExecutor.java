package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.Server;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;
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
            SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(pipeName);
            channel.connect(socketAddress);
            InputStream is = Channels.newInputStream(channel);
            OutputStream os = Channels.newOutputStream(channel);
            myTransport = new TFramedTransport(new TIOStreamTransport(is, os));
            myProtocol = new TBinaryProtocol(myTransport);
            myServer = new Server.Client(myProtocol);
        } catch (TTransportException e) {
            myTransport = null;
            throw e;
        } catch (IOException e) {
            myTransport = null;
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
    void closeSocket() {
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
