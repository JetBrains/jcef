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

    public RpcExecutor openTransport() throws TTransportException {
        if (ThriftTransport.isTcp())
            initTcp(ThriftTransport.getServerPort());
        else
            initPipe(ThriftTransport.getServerPipe().toString());
        return this;
    }

    private void initTcp(int port) throws TTransportException {
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

    private void initPipe(String pipeName) throws TTransportException {
        try {
            myTransport = ThriftTransport.openPipeTransport(pipeName);
            myProtocol = new TBinaryProtocol(myTransport);
            myServer = new Server.Client(myProtocol);
        } catch (TTransportException e) {
            myTransport = null;
            throw e;
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
    public int connect() {
        if (myTransport == null)
            return -1;
        try {
            return ThriftTransport.isTcp() ?
                    myServer.connectTcp(ThriftTransport.getJavaHandlersPort()) :
                    myServer.connect(ThriftTransport.getJavaHandlersPipe());
        } catch (TException e) {
            onThriftException(e);
        }
        return -1;
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
    public void closeTransport() {
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
