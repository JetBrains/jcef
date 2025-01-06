package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.Server;
import com.jetbrains.cef.remote.thrift.TException;
import com.jetbrains.cef.remote.thrift.protocol.TBinaryProtocol;
import com.jetbrains.cef.remote.thrift.protocol.TProtocol;
import com.jetbrains.cef.remote.thrift.transport.TSocket;
import com.jetbrains.cef.remote.thrift.transport.TTransport;
import com.jetbrains.cef.remote.thrift.transport.TTransportException;
import org.cef.misc.CefLog;

import java.io.PrintWriter;
import java.io.StringWriter;

public class RpcExecutor {
    private TTransport myTransport;
    private TProtocol myProtocol;
    private Server.Iface myServer;

    public RpcExecutor() {}

    public RpcExecutor openTransport(ThriftTransport thriftServer) throws TTransportException {
        if (thriftServer.isTcp())
            openTcpTransport(thriftServer.getPort());
        else
            openPipeTransport(thriftServer);
        return this;
    }

    private void openTcpTransport(int port) throws TTransportException {
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

    public void openPipeTransport(ThriftTransport thriftServer) throws TTransportException {
        myTransport = thriftServer.openPipeTransport();
        myProtocol = new TBinaryProtocol(myTransport);
        myServer = new Server.Client(myProtocol);
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

    public boolean isValid() {
        TTransport t = myTransport;
        return t != null && t.isOpen();
    }

    synchronized
    public int connect(ThriftTransport thriftBackward, boolean asMaster) {
        if (myTransport == null)
            return -1;
        try {
            return thriftBackward.isTcp() ?
                    myServer.connectTcp(thriftBackward.getPort(), asMaster) :
                    myServer.connect(thriftBackward.getPipe(), asMaster);
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
