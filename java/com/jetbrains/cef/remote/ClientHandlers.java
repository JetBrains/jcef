package com.jetbrains.cef.remote;

public class ClientHandlers {

  public interface Iface {

    public int connect() throws org.apache.thrift.TException;

    public void log(java.lang.String msg) throws org.apache.thrift.TException;

    public java.nio.ByteBuffer getInfo(int bid, java.lang.String request, java.nio.ByteBuffer buffer) throws org.apache.thrift.TException;

    public void onPaint(int bid, boolean popup, java.nio.ByteBuffer dirtyRects, java.nio.ByteBuffer buffer, int width, int height) throws org.apache.thrift.TException;

  }

  public interface AsyncIface {

    public void connect(org.apache.thrift.async.AsyncMethodCallback<java.lang.Integer> resultHandler) throws org.apache.thrift.TException;

    public void log(java.lang.String msg, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler) throws org.apache.thrift.TException;

    public void getInfo(int bid, java.lang.String request, java.nio.ByteBuffer buffer, org.apache.thrift.async.AsyncMethodCallback<java.nio.ByteBuffer> resultHandler) throws org.apache.thrift.TException;

    public void onPaint(int bid, boolean popup, java.nio.ByteBuffer dirtyRects, java.nio.ByteBuffer buffer, int width, int height, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler) throws org.apache.thrift.TException;

  }

  public static class Client extends org.apache.thrift.TServiceClient implements Iface {
    public static class Factory implements org.apache.thrift.TServiceClientFactory<Client> {
      public Factory() {}
      public Client getClient(org.apache.thrift.protocol.TProtocol prot) {
        return new Client(prot);
      }
      public Client getClient(org.apache.thrift.protocol.TProtocol iprot, org.apache.thrift.protocol.TProtocol oprot) {
        return new Client(iprot, oprot);
      }
    }

    public Client(org.apache.thrift.protocol.TProtocol prot)
    {
      super(prot, prot);
    }

    public Client(org.apache.thrift.protocol.TProtocol iprot, org.apache.thrift.protocol.TProtocol oprot) {
      super(iprot, oprot);
    }

    public int connect() throws org.apache.thrift.TException
    {
      send_connect();
      return recv_connect();
    }

    public void send_connect() throws org.apache.thrift.TException
    {
      connect_args args = new connect_args();
      sendBase("connect", args);
    }

    public int recv_connect() throws org.apache.thrift.TException
    {
      connect_result result = new connect_result();
      receiveBase(result, "connect");
      if (result.isSetSuccess()) {
        return result.success;
      }
      throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "connect failed: unknown result");
    }

    public void log(java.lang.String msg) throws org.apache.thrift.TException
    {
      send_log(msg);
    }

    public void send_log(java.lang.String msg) throws org.apache.thrift.TException
    {
      log_args args = new log_args();
      args.setMsg(msg);
      sendBaseOneway("log", args);
    }

    public java.nio.ByteBuffer getInfo(int bid, java.lang.String request, java.nio.ByteBuffer buffer) throws org.apache.thrift.TException
    {
      send_getInfo(bid, request, buffer);
      return recv_getInfo();
    }

    public void send_getInfo(int bid, java.lang.String request, java.nio.ByteBuffer buffer) throws org.apache.thrift.TException
    {
      getInfo_args args = new getInfo_args();
      args.setBid(bid);
      args.setRequest(request);
      args.setBuffer(buffer);
      sendBase("getInfo", args);
    }

    public java.nio.ByteBuffer recv_getInfo() throws org.apache.thrift.TException
    {
      getInfo_result result = new getInfo_result();
      receiveBase(result, "getInfo");
      if (result.isSetSuccess()) {
        return result.success;
      }
      throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "getInfo failed: unknown result");
    }

    public void onPaint(int bid, boolean popup, java.nio.ByteBuffer dirtyRects, java.nio.ByteBuffer buffer, int width, int height) throws org.apache.thrift.TException
    {
      send_onPaint(bid, popup, dirtyRects, buffer, width, height);
    }

    public void send_onPaint(int bid, boolean popup, java.nio.ByteBuffer dirtyRects, java.nio.ByteBuffer buffer, int width, int height) throws org.apache.thrift.TException
    {
      onPaint_args args = new onPaint_args();
      args.setBid(bid);
      args.setPopup(popup);
      args.setDirtyRects(dirtyRects);
      args.setBuffer(buffer);
      args.setWidth(width);
      args.setHeight(height);
      sendBaseOneway("onPaint", args);
    }

  }
  public static class AsyncClient extends org.apache.thrift.async.TAsyncClient implements AsyncIface {
    public static class Factory implements org.apache.thrift.async.TAsyncClientFactory<AsyncClient> {
      private org.apache.thrift.async.TAsyncClientManager clientManager;
      private org.apache.thrift.protocol.TProtocolFactory protocolFactory;
      public Factory(org.apache.thrift.async.TAsyncClientManager clientManager, org.apache.thrift.protocol.TProtocolFactory protocolFactory) {
        this.clientManager = clientManager;
        this.protocolFactory = protocolFactory;
      }
      public AsyncClient getAsyncClient(org.apache.thrift.transport.TNonblockingTransport transport) {
        return new AsyncClient(protocolFactory, clientManager, transport);
      }
    }

    public AsyncClient(org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.async.TAsyncClientManager clientManager, org.apache.thrift.transport.TNonblockingTransport transport) {
      super(protocolFactory, clientManager, transport);
    }

    public void connect(org.apache.thrift.async.AsyncMethodCallback<java.lang.Integer> resultHandler) throws org.apache.thrift.TException {
      checkReady();
      connect_call method_call = new connect_call(resultHandler, this, ___protocolFactory, ___transport);
      this.___currentMethod = method_call;
      ___manager.call(method_call);
    }

    public static class connect_call extends org.apache.thrift.async.TAsyncMethodCall<java.lang.Integer> {
      public connect_call(org.apache.thrift.async.AsyncMethodCallback<java.lang.Integer> resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
        super(client, protocolFactory, transport, resultHandler, false);
      }

      public void write_args(org.apache.thrift.protocol.TProtocol prot) throws org.apache.thrift.TException {
        prot.writeMessageBegin(new org.apache.thrift.protocol.TMessage("connect", org.apache.thrift.protocol.TMessageType.CALL, 0));
        connect_args args = new connect_args();
        args.write(prot);
        prot.writeMessageEnd();
      }

      public java.lang.Integer getResult() throws org.apache.thrift.TException {
        if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
          throw new java.lang.IllegalStateException("Method call not finished!");
        }
        org.apache.thrift.transport.TMemoryInputTransport memoryTransport = new org.apache.thrift.transport.TMemoryInputTransport(getFrameBuffer().array());
        org.apache.thrift.protocol.TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
        return (new Client(prot)).recv_connect();
      }
    }

    public void log(java.lang.String msg, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler) throws org.apache.thrift.TException {
      checkReady();
      log_call method_call = new log_call(msg, resultHandler, this, ___protocolFactory, ___transport);
      this.___currentMethod = method_call;
      ___manager.call(method_call);
    }

    public static class log_call extends org.apache.thrift.async.TAsyncMethodCall<Void> {
      private java.lang.String msg;
      public log_call(java.lang.String msg, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
        super(client, protocolFactory, transport, resultHandler, true);
        this.msg = msg;
      }

      public void write_args(org.apache.thrift.protocol.TProtocol prot) throws org.apache.thrift.TException {
        prot.writeMessageBegin(new org.apache.thrift.protocol.TMessage("log", org.apache.thrift.protocol.TMessageType.ONEWAY, 0));
        log_args args = new log_args();
        args.setMsg(msg);
        args.write(prot);
        prot.writeMessageEnd();
      }

      public Void getResult() throws org.apache.thrift.TException {
        if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
          throw new java.lang.IllegalStateException("Method call not finished!");
        }
        org.apache.thrift.transport.TMemoryInputTransport memoryTransport = new org.apache.thrift.transport.TMemoryInputTransport(getFrameBuffer().array());
        org.apache.thrift.protocol.TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
        return null;
      }
    }

    public void getInfo(int bid, java.lang.String request, java.nio.ByteBuffer buffer, org.apache.thrift.async.AsyncMethodCallback<java.nio.ByteBuffer> resultHandler) throws org.apache.thrift.TException {
      checkReady();
      getInfo_call method_call = new getInfo_call(bid, request, buffer, resultHandler, this, ___protocolFactory, ___transport);
      this.___currentMethod = method_call;
      ___manager.call(method_call);
    }

    public static class getInfo_call extends org.apache.thrift.async.TAsyncMethodCall<java.nio.ByteBuffer> {
      private int bid;
      private java.lang.String request;
      private java.nio.ByteBuffer buffer;
      public getInfo_call(int bid, java.lang.String request, java.nio.ByteBuffer buffer, org.apache.thrift.async.AsyncMethodCallback<java.nio.ByteBuffer> resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
        super(client, protocolFactory, transport, resultHandler, false);
        this.bid = bid;
        this.request = request;
        this.buffer = buffer;
      }

      public void write_args(org.apache.thrift.protocol.TProtocol prot) throws org.apache.thrift.TException {
        prot.writeMessageBegin(new org.apache.thrift.protocol.TMessage("getInfo", org.apache.thrift.protocol.TMessageType.CALL, 0));
        getInfo_args args = new getInfo_args();
        args.setBid(bid);
        args.setRequest(request);
        args.setBuffer(buffer);
        args.write(prot);
        prot.writeMessageEnd();
      }

      public java.nio.ByteBuffer getResult() throws org.apache.thrift.TException {
        if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
          throw new java.lang.IllegalStateException("Method call not finished!");
        }
        org.apache.thrift.transport.TMemoryInputTransport memoryTransport = new org.apache.thrift.transport.TMemoryInputTransport(getFrameBuffer().array());
        org.apache.thrift.protocol.TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
        return (new Client(prot)).recv_getInfo();
      }
    }

    public void onPaint(int bid, boolean popup, java.nio.ByteBuffer dirtyRects, java.nio.ByteBuffer buffer, int width, int height, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler) throws org.apache.thrift.TException {
      checkReady();
      onPaint_call method_call = new onPaint_call(bid, popup, dirtyRects, buffer, width, height, resultHandler, this, ___protocolFactory, ___transport);
      this.___currentMethod = method_call;
      ___manager.call(method_call);
    }

    public static class onPaint_call extends org.apache.thrift.async.TAsyncMethodCall<Void> {
      private int bid;
      private boolean popup;
      private java.nio.ByteBuffer dirtyRects;
      private java.nio.ByteBuffer buffer;
      private int width;
      private int height;
      public onPaint_call(int bid, boolean popup, java.nio.ByteBuffer dirtyRects, java.nio.ByteBuffer buffer, int width, int height, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
        super(client, protocolFactory, transport, resultHandler, true);
        this.bid = bid;
        this.popup = popup;
        this.dirtyRects = dirtyRects;
        this.buffer = buffer;
        this.width = width;
        this.height = height;
      }

      public void write_args(org.apache.thrift.protocol.TProtocol prot) throws org.apache.thrift.TException {
        prot.writeMessageBegin(new org.apache.thrift.protocol.TMessage("onPaint", org.apache.thrift.protocol.TMessageType.ONEWAY, 0));
        onPaint_args args = new onPaint_args();
        args.setBid(bid);
        args.setPopup(popup);
        args.setDirtyRects(dirtyRects);
        args.setBuffer(buffer);
        args.setWidth(width);
        args.setHeight(height);
        args.write(prot);
        prot.writeMessageEnd();
      }

      public Void getResult() throws org.apache.thrift.TException {
        if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
          throw new java.lang.IllegalStateException("Method call not finished!");
        }
        org.apache.thrift.transport.TMemoryInputTransport memoryTransport = new org.apache.thrift.transport.TMemoryInputTransport(getFrameBuffer().array());
        org.apache.thrift.protocol.TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
        return null;
      }
    }

  }

  public static class Processor<I extends Iface> extends org.apache.thrift.TBaseProcessor<I> implements org.apache.thrift.TProcessor {
    private static final org.slf4j.Logger _LOGGER = org.slf4j.LoggerFactory.getLogger(Processor.class.getName());
    public Processor(I iface) {
      super(iface, getProcessMap(new java.util.HashMap<java.lang.String, org.apache.thrift.ProcessFunction<I, ? extends org.apache.thrift.TBase>>()));
    }

    protected Processor(I iface, java.util.Map<java.lang.String, org.apache.thrift.ProcessFunction<I, ? extends org.apache.thrift.TBase>> processMap) {
      super(iface, getProcessMap(processMap));
    }

    private static <I extends Iface> java.util.Map<java.lang.String,  org.apache.thrift.ProcessFunction<I, ? extends org.apache.thrift.TBase>> getProcessMap(java.util.Map<java.lang.String, org.apache.thrift.ProcessFunction<I, ? extends  org.apache.thrift.TBase>> processMap) {
      processMap.put("connect", new connect());
      processMap.put("log", new log());
      processMap.put("getInfo", new getInfo());
      processMap.put("onPaint", new onPaint());
      return processMap;
    }

    public static class connect<I extends Iface> extends org.apache.thrift.ProcessFunction<I, connect_args> {
      public connect() {
        super("connect");
      }

      public connect_args getEmptyArgsInstance() {
        return new connect_args();
      }

      protected boolean isOneway() {
        return false;
      }

      @Override
      protected boolean rethrowUnhandledExceptions() {
        return false;
      }

      public connect_result getResult(I iface, connect_args args) throws org.apache.thrift.TException {
        connect_result result = new connect_result();
        result.success = iface.connect();
        result.setSuccessIsSet(true);
        return result;
      }
    }

    public static class log<I extends Iface> extends org.apache.thrift.ProcessFunction<I, log_args> {
      public log() {
        super("log");
      }

      public log_args getEmptyArgsInstance() {
        return new log_args();
      }

      protected boolean isOneway() {
        return true;
      }

      @Override
      protected boolean rethrowUnhandledExceptions() {
        return false;
      }

      public org.apache.thrift.TBase getResult(I iface, log_args args) throws org.apache.thrift.TException {
        iface.log(args.msg);
        return null;
      }
    }

    public static class getInfo<I extends Iface> extends org.apache.thrift.ProcessFunction<I, getInfo_args> {
      public getInfo() {
        super("getInfo");
      }

      public getInfo_args getEmptyArgsInstance() {
        return new getInfo_args();
      }

      protected boolean isOneway() {
        return false;
      }

      @Override
      protected boolean rethrowUnhandledExceptions() {
        return false;
      }

      public getInfo_result getResult(I iface, getInfo_args args) throws org.apache.thrift.TException {
        getInfo_result result = new getInfo_result();
        result.success = iface.getInfo(args.bid, args.request, args.buffer);
        return result;
      }
    }

    public static class onPaint<I extends Iface> extends org.apache.thrift.ProcessFunction<I, onPaint_args> {
      public onPaint() {
        super("onPaint");
      }

      public onPaint_args getEmptyArgsInstance() {
        return new onPaint_args();
      }

      protected boolean isOneway() {
        return true;
      }

      @Override
      protected boolean rethrowUnhandledExceptions() {
        return false;
      }

      public org.apache.thrift.TBase getResult(I iface, onPaint_args args) throws org.apache.thrift.TException {
        iface.onPaint(args.bid, args.popup, args.dirtyRects, args.buffer, args.width, args.height);
        return null;
      }
    }

  }

  public static class AsyncProcessor<I extends AsyncIface> extends org.apache.thrift.TBaseAsyncProcessor<I> {
    private static final org.slf4j.Logger _LOGGER = org.slf4j.LoggerFactory.getLogger(AsyncProcessor.class.getName());
    public AsyncProcessor(I iface) {
      super(iface, getProcessMap(new java.util.HashMap<java.lang.String, org.apache.thrift.AsyncProcessFunction<I, ? extends org.apache.thrift.TBase, ?>>()));
    }

    protected AsyncProcessor(I iface, java.util.Map<java.lang.String,  org.apache.thrift.AsyncProcessFunction<I, ? extends  org.apache.thrift.TBase, ?>> processMap) {
      super(iface, getProcessMap(processMap));
    }

    private static <I extends AsyncIface> java.util.Map<java.lang.String,  org.apache.thrift.AsyncProcessFunction<I, ? extends  org.apache.thrift.TBase,?>> getProcessMap(java.util.Map<java.lang.String,  org.apache.thrift.AsyncProcessFunction<I, ? extends  org.apache.thrift.TBase, ?>> processMap) {
      processMap.put("connect", new connect());
      processMap.put("log", new log());
      processMap.put("getInfo", new getInfo());
      processMap.put("onPaint", new onPaint());
      return processMap;
    }

    public static class connect<I extends AsyncIface> extends org.apache.thrift.AsyncProcessFunction<I, connect_args, java.lang.Integer> {
      public connect() {
        super("connect");
      }

      public connect_args getEmptyArgsInstance() {
        return new connect_args();
      }

      public org.apache.thrift.async.AsyncMethodCallback<java.lang.Integer> getResultHandler(final org.apache.thrift.server.AbstractNonblockingServer.AsyncFrameBuffer fb, final int seqid) {
        final org.apache.thrift.AsyncProcessFunction fcall = this;
        return new org.apache.thrift.async.AsyncMethodCallback<java.lang.Integer>() { 
          public void onComplete(java.lang.Integer o) {
            connect_result result = new connect_result();
            result.success = o;
            result.setSuccessIsSet(true);
            try {
              fcall.sendResponse(fb, result, org.apache.thrift.protocol.TMessageType.REPLY,seqid);
            } catch (org.apache.thrift.transport.TTransportException e) {
              _LOGGER.error("TTransportException writing to internal frame buffer", e);
              fb.close();
            } catch (java.lang.Exception e) {
              _LOGGER.error("Exception writing to internal frame buffer", e);
              onError(e);
            }
          }
          public void onError(java.lang.Exception e) {
            byte msgType = org.apache.thrift.protocol.TMessageType.REPLY;
            org.apache.thrift.TSerializable msg;
            connect_result result = new connect_result();
            if (e instanceof org.apache.thrift.transport.TTransportException) {
              _LOGGER.error("TTransportException inside handler", e);
              fb.close();
              return;
            } else if (e instanceof org.apache.thrift.TApplicationException) {
              _LOGGER.error("TApplicationException inside handler", e);
              msgType = org.apache.thrift.protocol.TMessageType.EXCEPTION;
              msg = (org.apache.thrift.TApplicationException)e;
            } else {
              _LOGGER.error("Exception inside handler", e);
              msgType = org.apache.thrift.protocol.TMessageType.EXCEPTION;
              msg = new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.INTERNAL_ERROR, e.getMessage());
            }
            try {
              fcall.sendResponse(fb,msg,msgType,seqid);
            } catch (java.lang.Exception ex) {
              _LOGGER.error("Exception writing to internal frame buffer", ex);
              fb.close();
            }
          }
        };
      }

      protected boolean isOneway() {
        return false;
      }

      public void start(I iface, connect_args args, org.apache.thrift.async.AsyncMethodCallback<java.lang.Integer> resultHandler) throws org.apache.thrift.TException {
        iface.connect(resultHandler);
      }
    }

    public static class log<I extends AsyncIface> extends org.apache.thrift.AsyncProcessFunction<I, log_args, Void> {
      public log() {
        super("log");
      }

      public log_args getEmptyArgsInstance() {
        return new log_args();
      }

      public org.apache.thrift.async.AsyncMethodCallback<Void> getResultHandler(final org.apache.thrift.server.AbstractNonblockingServer.AsyncFrameBuffer fb, final int seqid) {
        final org.apache.thrift.AsyncProcessFunction fcall = this;
        return new org.apache.thrift.async.AsyncMethodCallback<Void>() { 
          public void onComplete(Void o) {
          }
          public void onError(java.lang.Exception e) {
            if (e instanceof org.apache.thrift.transport.TTransportException) {
              _LOGGER.error("TTransportException inside handler", e);
              fb.close();
            } else {
              _LOGGER.error("Exception inside oneway handler", e);
            }
          }
        };
      }

      protected boolean isOneway() {
        return true;
      }

      public void start(I iface, log_args args, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler) throws org.apache.thrift.TException {
        iface.log(args.msg,resultHandler);
      }
    }

    public static class getInfo<I extends AsyncIface> extends org.apache.thrift.AsyncProcessFunction<I, getInfo_args, java.nio.ByteBuffer> {
      public getInfo() {
        super("getInfo");
      }

      public getInfo_args getEmptyArgsInstance() {
        return new getInfo_args();
      }

      public org.apache.thrift.async.AsyncMethodCallback<java.nio.ByteBuffer> getResultHandler(final org.apache.thrift.server.AbstractNonblockingServer.AsyncFrameBuffer fb, final int seqid) {
        final org.apache.thrift.AsyncProcessFunction fcall = this;
        return new org.apache.thrift.async.AsyncMethodCallback<java.nio.ByteBuffer>() { 
          public void onComplete(java.nio.ByteBuffer o) {
            getInfo_result result = new getInfo_result();
            result.success = o;
            try {
              fcall.sendResponse(fb, result, org.apache.thrift.protocol.TMessageType.REPLY,seqid);
            } catch (org.apache.thrift.transport.TTransportException e) {
              _LOGGER.error("TTransportException writing to internal frame buffer", e);
              fb.close();
            } catch (java.lang.Exception e) {
              _LOGGER.error("Exception writing to internal frame buffer", e);
              onError(e);
            }
          }
          public void onError(java.lang.Exception e) {
            byte msgType = org.apache.thrift.protocol.TMessageType.REPLY;
            org.apache.thrift.TSerializable msg;
            getInfo_result result = new getInfo_result();
            if (e instanceof org.apache.thrift.transport.TTransportException) {
              _LOGGER.error("TTransportException inside handler", e);
              fb.close();
              return;
            } else if (e instanceof org.apache.thrift.TApplicationException) {
              _LOGGER.error("TApplicationException inside handler", e);
              msgType = org.apache.thrift.protocol.TMessageType.EXCEPTION;
              msg = (org.apache.thrift.TApplicationException)e;
            } else {
              _LOGGER.error("Exception inside handler", e);
              msgType = org.apache.thrift.protocol.TMessageType.EXCEPTION;
              msg = new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.INTERNAL_ERROR, e.getMessage());
            }
            try {
              fcall.sendResponse(fb,msg,msgType,seqid);
            } catch (java.lang.Exception ex) {
              _LOGGER.error("Exception writing to internal frame buffer", ex);
              fb.close();
            }
          }
        };
      }

      protected boolean isOneway() {
        return false;
      }

      public void start(I iface, getInfo_args args, org.apache.thrift.async.AsyncMethodCallback<java.nio.ByteBuffer> resultHandler) throws org.apache.thrift.TException {
        iface.getInfo(args.bid, args.request, args.buffer,resultHandler);
      }
    }

    public static class onPaint<I extends AsyncIface> extends org.apache.thrift.AsyncProcessFunction<I, onPaint_args, Void> {
      public onPaint() {
        super("onPaint");
      }

      public onPaint_args getEmptyArgsInstance() {
        return new onPaint_args();
      }

      public org.apache.thrift.async.AsyncMethodCallback<Void> getResultHandler(final org.apache.thrift.server.AbstractNonblockingServer.AsyncFrameBuffer fb, final int seqid) {
        final org.apache.thrift.AsyncProcessFunction fcall = this;
        return new org.apache.thrift.async.AsyncMethodCallback<Void>() { 
          public void onComplete(Void o) {
          }
          public void onError(java.lang.Exception e) {
            if (e instanceof org.apache.thrift.transport.TTransportException) {
              _LOGGER.error("TTransportException inside handler", e);
              fb.close();
            } else {
              _LOGGER.error("Exception inside oneway handler", e);
            }
          }
        };
      }

      protected boolean isOneway() {
        return true;
      }

      public void start(I iface, onPaint_args args, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler) throws org.apache.thrift.TException {
        iface.onPaint(args.bid, args.popup, args.dirtyRects, args.buffer, args.width, args.height,resultHandler);
      }
    }

  }

  public static class connect_args implements org.apache.thrift.TBase<connect_args, connect_args._Fields>, java.io.Serializable, Cloneable, Comparable<connect_args>   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("connect_args");


    private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new connect_argsStandardSchemeFactory();
    private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new connect_argsTupleSchemeFactory();


    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
;

      private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

      static {
        for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
          byName.put(field.getFieldName(), field);
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByThriftId(int fieldId) {
        switch(fieldId) {
          default:
            return null;
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, throwing an exception
       * if it is not found.
       */
      public static _Fields findByThriftIdOrThrow(int fieldId) {
        _Fields fields = findByThriftId(fieldId);
        if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
        return fields;
      }

      /**
       * Find the _Fields constant that matches name, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByName(java.lang.String name) {
        return byName.get(name);
      }

      private final short _thriftId;
      private final java.lang.String _fieldName;

      _Fields(short thriftId, java.lang.String fieldName) {
        _thriftId = thriftId;
        _fieldName = fieldName;
      }

      public short getThriftFieldId() {
        return _thriftId;
      }

      public java.lang.String getFieldName() {
        return _fieldName;
      }
    }
    public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(connect_args.class, metaDataMap);
    }

    public connect_args() {
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public connect_args(connect_args other) {
    }

    public connect_args deepCopy() {
      return new connect_args(this);
    }

    @Override
    public void clear() {
    }

    public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
      switch (field) {
      }
    }

    @org.apache.thrift.annotation.Nullable
    public java.lang.Object getFieldValue(_Fields field) {
      switch (field) {
      }
      throw new java.lang.IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new java.lang.IllegalArgumentException();
      }

      switch (field) {
      }
      throw new java.lang.IllegalStateException();
    }

    @Override
    public boolean equals(java.lang.Object that) {
      if (that instanceof connect_args)
        return this.equals((connect_args)that);
      return false;
    }

    public boolean equals(connect_args that) {
      if (that == null)
        return false;
      if (this == that)
        return true;

      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = 1;

      return hashCode;
    }

    @Override
    public int compareTo(connect_args other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;

      return 0;
    }

    @org.apache.thrift.annotation.Nullable
    public _Fields fieldForId(int fieldId) {
      return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
      scheme(iprot).read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
      scheme(oprot).write(oprot, this);
    }

    @Override
    public java.lang.String toString() {
      java.lang.StringBuilder sb = new java.lang.StringBuilder("connect_args(");
      boolean first = true;

      sb.append(")");
      return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
      // check for required fields
      // check for sub-struct validity
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      try {
        write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
      try {
        read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private static class connect_argsStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public connect_argsStandardScheme getScheme() {
        return new connect_argsStandardScheme();
      }
    }

    private static class connect_argsStandardScheme extends org.apache.thrift.scheme.StandardScheme<connect_args> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, connect_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            default:
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          }
          iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        // check for required fields of primitive type, which can't be checked in the validate method
        struct.validate();
      }

      public void write(org.apache.thrift.protocol.TProtocol oprot, connect_args struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class connect_argsTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public connect_argsTupleScheme getScheme() {
        return new connect_argsTupleScheme();
      }
    }

    private static class connect_argsTupleScheme extends org.apache.thrift.scheme.TupleScheme<connect_args> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, connect_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, connect_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      }
    }

    private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
      return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
    }
  }

  public static class connect_result implements org.apache.thrift.TBase<connect_result, connect_result._Fields>, java.io.Serializable, Cloneable, Comparable<connect_result>   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("connect_result");

    private static final org.apache.thrift.protocol.TField SUCCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("success", org.apache.thrift.protocol.TType.I32, (short)0);

    private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new connect_resultStandardSchemeFactory();
    private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new connect_resultTupleSchemeFactory();

    public int success; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      SUCCESS((short)0, "success");

      private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

      static {
        for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
          byName.put(field.getFieldName(), field);
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByThriftId(int fieldId) {
        switch(fieldId) {
          case 0: // SUCCESS
            return SUCCESS;
          default:
            return null;
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, throwing an exception
       * if it is not found.
       */
      public static _Fields findByThriftIdOrThrow(int fieldId) {
        _Fields fields = findByThriftId(fieldId);
        if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
        return fields;
      }

      /**
       * Find the _Fields constant that matches name, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByName(java.lang.String name) {
        return byName.get(name);
      }

      private final short _thriftId;
      private final java.lang.String _fieldName;

      _Fields(short thriftId, java.lang.String fieldName) {
        _thriftId = thriftId;
        _fieldName = fieldName;
      }

      public short getThriftFieldId() {
        return _thriftId;
      }

      public java.lang.String getFieldName() {
        return _fieldName;
      }
    }

    // isset id assignments
    private static final int __SUCCESS_ISSET_ID = 0;
    private byte __isset_bitfield = 0;
    public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.SUCCESS, new org.apache.thrift.meta_data.FieldMetaData("success", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
      metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(connect_result.class, metaDataMap);
    }

    public connect_result() {
    }

    public connect_result(
      int success)
    {
      this();
      this.success = success;
      setSuccessIsSet(true);
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public connect_result(connect_result other) {
      __isset_bitfield = other.__isset_bitfield;
      this.success = other.success;
    }

    public connect_result deepCopy() {
      return new connect_result(this);
    }

    @Override
    public void clear() {
      setSuccessIsSet(false);
      this.success = 0;
    }

    public int getSuccess() {
      return this.success;
    }

    public connect_result setSuccess(int success) {
      this.success = success;
      setSuccessIsSet(true);
      return this;
    }

    public void unsetSuccess() {
      __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __SUCCESS_ISSET_ID);
    }

    /** Returns true if field success is set (has been assigned a value) and false otherwise */
    public boolean isSetSuccess() {
      return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __SUCCESS_ISSET_ID);
    }

    public void setSuccessIsSet(boolean value) {
      __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __SUCCESS_ISSET_ID, value);
    }

    public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
      switch (field) {
      case SUCCESS:
        if (value == null) {
          unsetSuccess();
        } else {
          setSuccess((java.lang.Integer)value);
        }
        break;

      }
    }

    @org.apache.thrift.annotation.Nullable
    public java.lang.Object getFieldValue(_Fields field) {
      switch (field) {
      case SUCCESS:
        return getSuccess();

      }
      throw new java.lang.IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new java.lang.IllegalArgumentException();
      }

      switch (field) {
      case SUCCESS:
        return isSetSuccess();
      }
      throw new java.lang.IllegalStateException();
    }

    @Override
    public boolean equals(java.lang.Object that) {
      if (that instanceof connect_result)
        return this.equals((connect_result)that);
      return false;
    }

    public boolean equals(connect_result that) {
      if (that == null)
        return false;
      if (this == that)
        return true;

      boolean this_present_success = true;
      boolean that_present_success = true;
      if (this_present_success || that_present_success) {
        if (!(this_present_success && that_present_success))
          return false;
        if (this.success != that.success)
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = 1;

      hashCode = hashCode * 8191 + success;

      return hashCode;
    }

    @Override
    public int compareTo(connect_result other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;

      lastComparison = java.lang.Boolean.compare(isSetSuccess(), other.isSetSuccess());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetSuccess()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.success, other.success);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      return 0;
    }

    @org.apache.thrift.annotation.Nullable
    public _Fields fieldForId(int fieldId) {
      return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
      scheme(iprot).read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
      scheme(oprot).write(oprot, this);
      }

    @Override
    public java.lang.String toString() {
      java.lang.StringBuilder sb = new java.lang.StringBuilder("connect_result(");
      boolean first = true;

      sb.append("success:");
      sb.append(this.success);
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
      // check for required fields
      // check for sub-struct validity
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      try {
        write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
      try {
        // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
        __isset_bitfield = 0;
        read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private static class connect_resultStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public connect_resultStandardScheme getScheme() {
        return new connect_resultStandardScheme();
      }
    }

    private static class connect_resultStandardScheme extends org.apache.thrift.scheme.StandardScheme<connect_result> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, connect_result struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 0: // SUCCESS
              if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
                struct.success = iprot.readI32();
                struct.setSuccessIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            default:
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          }
          iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        // check for required fields of primitive type, which can't be checked in the validate method
        struct.validate();
      }

      public void write(org.apache.thrift.protocol.TProtocol oprot, connect_result struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        if (struct.isSetSuccess()) {
          oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
          oprot.writeI32(struct.success);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class connect_resultTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public connect_resultTupleScheme getScheme() {
        return new connect_resultTupleScheme();
      }
    }

    private static class connect_resultTupleScheme extends org.apache.thrift.scheme.TupleScheme<connect_result> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, connect_result struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet optionals = new java.util.BitSet();
        if (struct.isSetSuccess()) {
          optionals.set(0);
        }
        oprot.writeBitSet(optionals, 1);
        if (struct.isSetSuccess()) {
          oprot.writeI32(struct.success);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, connect_result struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet incoming = iprot.readBitSet(1);
        if (incoming.get(0)) {
          struct.success = iprot.readI32();
          struct.setSuccessIsSet(true);
        }
      }
    }

    private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
      return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
    }
  }

  public static class log_args implements org.apache.thrift.TBase<log_args, log_args._Fields>, java.io.Serializable, Cloneable, Comparable<log_args>   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("log_args");

    private static final org.apache.thrift.protocol.TField MSG_FIELD_DESC = new org.apache.thrift.protocol.TField("msg", org.apache.thrift.protocol.TType.STRING, (short)1);

    private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new log_argsStandardSchemeFactory();
    private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new log_argsTupleSchemeFactory();

    public @org.apache.thrift.annotation.Nullable java.lang.String msg; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      MSG((short)1, "msg");

      private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

      static {
        for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
          byName.put(field.getFieldName(), field);
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByThriftId(int fieldId) {
        switch(fieldId) {
          case 1: // MSG
            return MSG;
          default:
            return null;
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, throwing an exception
       * if it is not found.
       */
      public static _Fields findByThriftIdOrThrow(int fieldId) {
        _Fields fields = findByThriftId(fieldId);
        if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
        return fields;
      }

      /**
       * Find the _Fields constant that matches name, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByName(java.lang.String name) {
        return byName.get(name);
      }

      private final short _thriftId;
      private final java.lang.String _fieldName;

      _Fields(short thriftId, java.lang.String fieldName) {
        _thriftId = thriftId;
        _fieldName = fieldName;
      }

      public short getThriftFieldId() {
        return _thriftId;
      }

      public java.lang.String getFieldName() {
        return _fieldName;
      }
    }

    // isset id assignments
    public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.MSG, new org.apache.thrift.meta_data.FieldMetaData("msg", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
      metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(log_args.class, metaDataMap);
    }

    public log_args() {
    }

    public log_args(
      java.lang.String msg)
    {
      this();
      this.msg = msg;
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public log_args(log_args other) {
      if (other.isSetMsg()) {
        this.msg = other.msg;
      }
    }

    public log_args deepCopy() {
      return new log_args(this);
    }

    @Override
    public void clear() {
      this.msg = null;
    }

    @org.apache.thrift.annotation.Nullable
    public java.lang.String getMsg() {
      return this.msg;
    }

    public log_args setMsg(@org.apache.thrift.annotation.Nullable java.lang.String msg) {
      this.msg = msg;
      return this;
    }

    public void unsetMsg() {
      this.msg = null;
    }

    /** Returns true if field msg is set (has been assigned a value) and false otherwise */
    public boolean isSetMsg() {
      return this.msg != null;
    }

    public void setMsgIsSet(boolean value) {
      if (!value) {
        this.msg = null;
      }
    }

    public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
      switch (field) {
      case MSG:
        if (value == null) {
          unsetMsg();
        } else {
          setMsg((java.lang.String)value);
        }
        break;

      }
    }

    @org.apache.thrift.annotation.Nullable
    public java.lang.Object getFieldValue(_Fields field) {
      switch (field) {
      case MSG:
        return getMsg();

      }
      throw new java.lang.IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new java.lang.IllegalArgumentException();
      }

      switch (field) {
      case MSG:
        return isSetMsg();
      }
      throw new java.lang.IllegalStateException();
    }

    @Override
    public boolean equals(java.lang.Object that) {
      if (that instanceof log_args)
        return this.equals((log_args)that);
      return false;
    }

    public boolean equals(log_args that) {
      if (that == null)
        return false;
      if (this == that)
        return true;

      boolean this_present_msg = true && this.isSetMsg();
      boolean that_present_msg = true && that.isSetMsg();
      if (this_present_msg || that_present_msg) {
        if (!(this_present_msg && that_present_msg))
          return false;
        if (!this.msg.equals(that.msg))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = 1;

      hashCode = hashCode * 8191 + ((isSetMsg()) ? 131071 : 524287);
      if (isSetMsg())
        hashCode = hashCode * 8191 + msg.hashCode();

      return hashCode;
    }

    @Override
    public int compareTo(log_args other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;

      lastComparison = java.lang.Boolean.compare(isSetMsg(), other.isSetMsg());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetMsg()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.msg, other.msg);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      return 0;
    }

    @org.apache.thrift.annotation.Nullable
    public _Fields fieldForId(int fieldId) {
      return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
      scheme(iprot).read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
      scheme(oprot).write(oprot, this);
    }

    @Override
    public java.lang.String toString() {
      java.lang.StringBuilder sb = new java.lang.StringBuilder("log_args(");
      boolean first = true;

      sb.append("msg:");
      if (this.msg == null) {
        sb.append("null");
      } else {
        sb.append(this.msg);
      }
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
      // check for required fields
      // check for sub-struct validity
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      try {
        write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
      try {
        read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private static class log_argsStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public log_argsStandardScheme getScheme() {
        return new log_argsStandardScheme();
      }
    }

    private static class log_argsStandardScheme extends org.apache.thrift.scheme.StandardScheme<log_args> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, log_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 1: // MSG
              if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
                struct.msg = iprot.readString();
                struct.setMsgIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            default:
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          }
          iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        // check for required fields of primitive type, which can't be checked in the validate method
        struct.validate();
      }

      public void write(org.apache.thrift.protocol.TProtocol oprot, log_args struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        if (struct.msg != null) {
          oprot.writeFieldBegin(MSG_FIELD_DESC);
          oprot.writeString(struct.msg);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class log_argsTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public log_argsTupleScheme getScheme() {
        return new log_argsTupleScheme();
      }
    }

    private static class log_argsTupleScheme extends org.apache.thrift.scheme.TupleScheme<log_args> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, log_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet optionals = new java.util.BitSet();
        if (struct.isSetMsg()) {
          optionals.set(0);
        }
        oprot.writeBitSet(optionals, 1);
        if (struct.isSetMsg()) {
          oprot.writeString(struct.msg);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, log_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet incoming = iprot.readBitSet(1);
        if (incoming.get(0)) {
          struct.msg = iprot.readString();
          struct.setMsgIsSet(true);
        }
      }
    }

    private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
      return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
    }
  }

  public static class getInfo_args implements org.apache.thrift.TBase<getInfo_args, getInfo_args._Fields>, java.io.Serializable, Cloneable, Comparable<getInfo_args>   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("getInfo_args");

    private static final org.apache.thrift.protocol.TField BID_FIELD_DESC = new org.apache.thrift.protocol.TField("bid", org.apache.thrift.protocol.TType.I32, (short)1);
    private static final org.apache.thrift.protocol.TField REQUEST_FIELD_DESC = new org.apache.thrift.protocol.TField("request", org.apache.thrift.protocol.TType.STRING, (short)2);
    private static final org.apache.thrift.protocol.TField BUFFER_FIELD_DESC = new org.apache.thrift.protocol.TField("buffer", org.apache.thrift.protocol.TType.STRING, (short)3);

    private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new getInfo_argsStandardSchemeFactory();
    private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new getInfo_argsTupleSchemeFactory();

    public int bid; // required
    public @org.apache.thrift.annotation.Nullable java.lang.String request; // required
    public @org.apache.thrift.annotation.Nullable java.nio.ByteBuffer buffer; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      BID((short)1, "bid"),
      REQUEST((short)2, "request"),
      BUFFER((short)3, "buffer");

      private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

      static {
        for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
          byName.put(field.getFieldName(), field);
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByThriftId(int fieldId) {
        switch(fieldId) {
          case 1: // BID
            return BID;
          case 2: // REQUEST
            return REQUEST;
          case 3: // BUFFER
            return BUFFER;
          default:
            return null;
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, throwing an exception
       * if it is not found.
       */
      public static _Fields findByThriftIdOrThrow(int fieldId) {
        _Fields fields = findByThriftId(fieldId);
        if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
        return fields;
      }

      /**
       * Find the _Fields constant that matches name, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByName(java.lang.String name) {
        return byName.get(name);
      }

      private final short _thriftId;
      private final java.lang.String _fieldName;

      _Fields(short thriftId, java.lang.String fieldName) {
        _thriftId = thriftId;
        _fieldName = fieldName;
      }

      public short getThriftFieldId() {
        return _thriftId;
      }

      public java.lang.String getFieldName() {
        return _fieldName;
      }
    }

    // isset id assignments
    private static final int __BID_ISSET_ID = 0;
    private byte __isset_bitfield = 0;
    public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.BID, new org.apache.thrift.meta_data.FieldMetaData("bid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
      tmpMap.put(_Fields.REQUEST, new org.apache.thrift.meta_data.FieldMetaData("request", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
      tmpMap.put(_Fields.BUFFER, new org.apache.thrift.meta_data.FieldMetaData("buffer", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING          , true)));
      metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(getInfo_args.class, metaDataMap);
    }

    public getInfo_args() {
    }

    public getInfo_args(
      int bid,
      java.lang.String request,
      java.nio.ByteBuffer buffer)
    {
      this();
      this.bid = bid;
      setBidIsSet(true);
      this.request = request;
      this.buffer = org.apache.thrift.TBaseHelper.copyBinary(buffer);
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public getInfo_args(getInfo_args other) {
      __isset_bitfield = other.__isset_bitfield;
      this.bid = other.bid;
      if (other.isSetRequest()) {
        this.request = other.request;
      }
      if (other.isSetBuffer()) {
        this.buffer = org.apache.thrift.TBaseHelper.copyBinary(other.buffer);
      }
    }

    public getInfo_args deepCopy() {
      return new getInfo_args(this);
    }

    @Override
    public void clear() {
      setBidIsSet(false);
      this.bid = 0;
      this.request = null;
      this.buffer = null;
    }

    public int getBid() {
      return this.bid;
    }

    public getInfo_args setBid(int bid) {
      this.bid = bid;
      setBidIsSet(true);
      return this;
    }

    public void unsetBid() {
      __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __BID_ISSET_ID);
    }

    /** Returns true if field bid is set (has been assigned a value) and false otherwise */
    public boolean isSetBid() {
      return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __BID_ISSET_ID);
    }

    public void setBidIsSet(boolean value) {
      __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __BID_ISSET_ID, value);
    }

    @org.apache.thrift.annotation.Nullable
    public java.lang.String getRequest() {
      return this.request;
    }

    public getInfo_args setRequest(@org.apache.thrift.annotation.Nullable java.lang.String request) {
      this.request = request;
      return this;
    }

    public void unsetRequest() {
      this.request = null;
    }

    /** Returns true if field request is set (has been assigned a value) and false otherwise */
    public boolean isSetRequest() {
      return this.request != null;
    }

    public void setRequestIsSet(boolean value) {
      if (!value) {
        this.request = null;
      }
    }

    public byte[] getBuffer() {
      setBuffer(org.apache.thrift.TBaseHelper.rightSize(buffer));
      return buffer == null ? null : buffer.array();
    }

    public java.nio.ByteBuffer bufferForBuffer() {
      return org.apache.thrift.TBaseHelper.copyBinary(buffer);
    }

    public getInfo_args setBuffer(byte[] buffer) {
      this.buffer = buffer == null ? (java.nio.ByteBuffer)null     : java.nio.ByteBuffer.wrap(buffer.clone());
      return this;
    }

    public getInfo_args setBuffer(@org.apache.thrift.annotation.Nullable java.nio.ByteBuffer buffer) {
      this.buffer = org.apache.thrift.TBaseHelper.copyBinary(buffer);
      return this;
    }

    public void unsetBuffer() {
      this.buffer = null;
    }

    /** Returns true if field buffer is set (has been assigned a value) and false otherwise */
    public boolean isSetBuffer() {
      return this.buffer != null;
    }

    public void setBufferIsSet(boolean value) {
      if (!value) {
        this.buffer = null;
      }
    }

    public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
      switch (field) {
      case BID:
        if (value == null) {
          unsetBid();
        } else {
          setBid((java.lang.Integer)value);
        }
        break;

      case REQUEST:
        if (value == null) {
          unsetRequest();
        } else {
          setRequest((java.lang.String)value);
        }
        break;

      case BUFFER:
        if (value == null) {
          unsetBuffer();
        } else {
          if (value instanceof byte[]) {
            setBuffer((byte[])value);
          } else {
            setBuffer((java.nio.ByteBuffer)value);
          }
        }
        break;

      }
    }

    @org.apache.thrift.annotation.Nullable
    public java.lang.Object getFieldValue(_Fields field) {
      switch (field) {
      case BID:
        return getBid();

      case REQUEST:
        return getRequest();

      case BUFFER:
        return getBuffer();

      }
      throw new java.lang.IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new java.lang.IllegalArgumentException();
      }

      switch (field) {
      case BID:
        return isSetBid();
      case REQUEST:
        return isSetRequest();
      case BUFFER:
        return isSetBuffer();
      }
      throw new java.lang.IllegalStateException();
    }

    @Override
    public boolean equals(java.lang.Object that) {
      if (that instanceof getInfo_args)
        return this.equals((getInfo_args)that);
      return false;
    }

    public boolean equals(getInfo_args that) {
      if (that == null)
        return false;
      if (this == that)
        return true;

      boolean this_present_bid = true;
      boolean that_present_bid = true;
      if (this_present_bid || that_present_bid) {
        if (!(this_present_bid && that_present_bid))
          return false;
        if (this.bid != that.bid)
          return false;
      }

      boolean this_present_request = true && this.isSetRequest();
      boolean that_present_request = true && that.isSetRequest();
      if (this_present_request || that_present_request) {
        if (!(this_present_request && that_present_request))
          return false;
        if (!this.request.equals(that.request))
          return false;
      }

      boolean this_present_buffer = true && this.isSetBuffer();
      boolean that_present_buffer = true && that.isSetBuffer();
      if (this_present_buffer || that_present_buffer) {
        if (!(this_present_buffer && that_present_buffer))
          return false;
        if (!this.buffer.equals(that.buffer))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = 1;

      hashCode = hashCode * 8191 + bid;

      hashCode = hashCode * 8191 + ((isSetRequest()) ? 131071 : 524287);
      if (isSetRequest())
        hashCode = hashCode * 8191 + request.hashCode();

      hashCode = hashCode * 8191 + ((isSetBuffer()) ? 131071 : 524287);
      if (isSetBuffer())
        hashCode = hashCode * 8191 + buffer.hashCode();

      return hashCode;
    }

    @Override
    public int compareTo(getInfo_args other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;

      lastComparison = java.lang.Boolean.compare(isSetBid(), other.isSetBid());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetBid()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.bid, other.bid);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = java.lang.Boolean.compare(isSetRequest(), other.isSetRequest());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetRequest()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.request, other.request);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = java.lang.Boolean.compare(isSetBuffer(), other.isSetBuffer());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetBuffer()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.buffer, other.buffer);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      return 0;
    }

    @org.apache.thrift.annotation.Nullable
    public _Fields fieldForId(int fieldId) {
      return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
      scheme(iprot).read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
      scheme(oprot).write(oprot, this);
    }

    @Override
    public java.lang.String toString() {
      java.lang.StringBuilder sb = new java.lang.StringBuilder("getInfo_args(");
      boolean first = true;

      sb.append("bid:");
      sb.append(this.bid);
      first = false;
      if (!first) sb.append(", ");
      sb.append("request:");
      if (this.request == null) {
        sb.append("null");
      } else {
        sb.append(this.request);
      }
      first = false;
      if (!first) sb.append(", ");
      sb.append("buffer:");
      if (this.buffer == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.buffer, sb);
      }
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
      // check for required fields
      // check for sub-struct validity
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      try {
        write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
      try {
        // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
        __isset_bitfield = 0;
        read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private static class getInfo_argsStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public getInfo_argsStandardScheme getScheme() {
        return new getInfo_argsStandardScheme();
      }
    }

    private static class getInfo_argsStandardScheme extends org.apache.thrift.scheme.StandardScheme<getInfo_args> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, getInfo_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 1: // BID
              if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
                struct.bid = iprot.readI32();
                struct.setBidIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 2: // REQUEST
              if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
                struct.request = iprot.readString();
                struct.setRequestIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 3: // BUFFER
              if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
                struct.buffer = iprot.readBinary();
                struct.setBufferIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            default:
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          }
          iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        // check for required fields of primitive type, which can't be checked in the validate method
        struct.validate();
      }

      public void write(org.apache.thrift.protocol.TProtocol oprot, getInfo_args struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        oprot.writeFieldBegin(BID_FIELD_DESC);
        oprot.writeI32(struct.bid);
        oprot.writeFieldEnd();
        if (struct.request != null) {
          oprot.writeFieldBegin(REQUEST_FIELD_DESC);
          oprot.writeString(struct.request);
          oprot.writeFieldEnd();
        }
        if (struct.buffer != null) {
          oprot.writeFieldBegin(BUFFER_FIELD_DESC);
          oprot.writeBinary(struct.buffer);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class getInfo_argsTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public getInfo_argsTupleScheme getScheme() {
        return new getInfo_argsTupleScheme();
      }
    }

    private static class getInfo_argsTupleScheme extends org.apache.thrift.scheme.TupleScheme<getInfo_args> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, getInfo_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet optionals = new java.util.BitSet();
        if (struct.isSetBid()) {
          optionals.set(0);
        }
        if (struct.isSetRequest()) {
          optionals.set(1);
        }
        if (struct.isSetBuffer()) {
          optionals.set(2);
        }
        oprot.writeBitSet(optionals, 3);
        if (struct.isSetBid()) {
          oprot.writeI32(struct.bid);
        }
        if (struct.isSetRequest()) {
          oprot.writeString(struct.request);
        }
        if (struct.isSetBuffer()) {
          oprot.writeBinary(struct.buffer);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, getInfo_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet incoming = iprot.readBitSet(3);
        if (incoming.get(0)) {
          struct.bid = iprot.readI32();
          struct.setBidIsSet(true);
        }
        if (incoming.get(1)) {
          struct.request = iprot.readString();
          struct.setRequestIsSet(true);
        }
        if (incoming.get(2)) {
          struct.buffer = iprot.readBinary();
          struct.setBufferIsSet(true);
        }
      }
    }

    private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
      return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
    }
  }

  public static class getInfo_result implements org.apache.thrift.TBase<getInfo_result, getInfo_result._Fields>, java.io.Serializable, Cloneable, Comparable<getInfo_result>   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("getInfo_result");

    private static final org.apache.thrift.protocol.TField SUCCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("success", org.apache.thrift.protocol.TType.STRING, (short)0);

    private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new getInfo_resultStandardSchemeFactory();
    private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new getInfo_resultTupleSchemeFactory();

    public @org.apache.thrift.annotation.Nullable java.nio.ByteBuffer success; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      SUCCESS((short)0, "success");

      private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

      static {
        for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
          byName.put(field.getFieldName(), field);
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByThriftId(int fieldId) {
        switch(fieldId) {
          case 0: // SUCCESS
            return SUCCESS;
          default:
            return null;
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, throwing an exception
       * if it is not found.
       */
      public static _Fields findByThriftIdOrThrow(int fieldId) {
        _Fields fields = findByThriftId(fieldId);
        if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
        return fields;
      }

      /**
       * Find the _Fields constant that matches name, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByName(java.lang.String name) {
        return byName.get(name);
      }

      private final short _thriftId;
      private final java.lang.String _fieldName;

      _Fields(short thriftId, java.lang.String fieldName) {
        _thriftId = thriftId;
        _fieldName = fieldName;
      }

      public short getThriftFieldId() {
        return _thriftId;
      }

      public java.lang.String getFieldName() {
        return _fieldName;
      }
    }

    // isset id assignments
    public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.SUCCESS, new org.apache.thrift.meta_data.FieldMetaData("success", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING          , true)));
      metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(getInfo_result.class, metaDataMap);
    }

    public getInfo_result() {
    }

    public getInfo_result(
      java.nio.ByteBuffer success)
    {
      this();
      this.success = org.apache.thrift.TBaseHelper.copyBinary(success);
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public getInfo_result(getInfo_result other) {
      if (other.isSetSuccess()) {
        this.success = org.apache.thrift.TBaseHelper.copyBinary(other.success);
      }
    }

    public getInfo_result deepCopy() {
      return new getInfo_result(this);
    }

    @Override
    public void clear() {
      this.success = null;
    }

    public byte[] getSuccess() {
      setSuccess(org.apache.thrift.TBaseHelper.rightSize(success));
      return success == null ? null : success.array();
    }

    public java.nio.ByteBuffer bufferForSuccess() {
      return org.apache.thrift.TBaseHelper.copyBinary(success);
    }

    public getInfo_result setSuccess(byte[] success) {
      this.success = success == null ? (java.nio.ByteBuffer)null     : java.nio.ByteBuffer.wrap(success.clone());
      return this;
    }

    public getInfo_result setSuccess(@org.apache.thrift.annotation.Nullable java.nio.ByteBuffer success) {
      this.success = org.apache.thrift.TBaseHelper.copyBinary(success);
      return this;
    }

    public void unsetSuccess() {
      this.success = null;
    }

    /** Returns true if field success is set (has been assigned a value) and false otherwise */
    public boolean isSetSuccess() {
      return this.success != null;
    }

    public void setSuccessIsSet(boolean value) {
      if (!value) {
        this.success = null;
      }
    }

    public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
      switch (field) {
      case SUCCESS:
        if (value == null) {
          unsetSuccess();
        } else {
          if (value instanceof byte[]) {
            setSuccess((byte[])value);
          } else {
            setSuccess((java.nio.ByteBuffer)value);
          }
        }
        break;

      }
    }

    @org.apache.thrift.annotation.Nullable
    public java.lang.Object getFieldValue(_Fields field) {
      switch (field) {
      case SUCCESS:
        return getSuccess();

      }
      throw new java.lang.IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new java.lang.IllegalArgumentException();
      }

      switch (field) {
      case SUCCESS:
        return isSetSuccess();
      }
      throw new java.lang.IllegalStateException();
    }

    @Override
    public boolean equals(java.lang.Object that) {
      if (that instanceof getInfo_result)
        return this.equals((getInfo_result)that);
      return false;
    }

    public boolean equals(getInfo_result that) {
      if (that == null)
        return false;
      if (this == that)
        return true;

      boolean this_present_success = true && this.isSetSuccess();
      boolean that_present_success = true && that.isSetSuccess();
      if (this_present_success || that_present_success) {
        if (!(this_present_success && that_present_success))
          return false;
        if (!this.success.equals(that.success))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = 1;

      hashCode = hashCode * 8191 + ((isSetSuccess()) ? 131071 : 524287);
      if (isSetSuccess())
        hashCode = hashCode * 8191 + success.hashCode();

      return hashCode;
    }

    @Override
    public int compareTo(getInfo_result other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;

      lastComparison = java.lang.Boolean.compare(isSetSuccess(), other.isSetSuccess());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetSuccess()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.success, other.success);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      return 0;
    }

    @org.apache.thrift.annotation.Nullable
    public _Fields fieldForId(int fieldId) {
      return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
      scheme(iprot).read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
      scheme(oprot).write(oprot, this);
      }

    @Override
    public java.lang.String toString() {
      java.lang.StringBuilder sb = new java.lang.StringBuilder("getInfo_result(");
      boolean first = true;

      sb.append("success:");
      if (this.success == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.success, sb);
      }
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
      // check for required fields
      // check for sub-struct validity
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      try {
        write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
      try {
        read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private static class getInfo_resultStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public getInfo_resultStandardScheme getScheme() {
        return new getInfo_resultStandardScheme();
      }
    }

    private static class getInfo_resultStandardScheme extends org.apache.thrift.scheme.StandardScheme<getInfo_result> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, getInfo_result struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 0: // SUCCESS
              if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
                struct.success = iprot.readBinary();
                struct.setSuccessIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            default:
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          }
          iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        // check for required fields of primitive type, which can't be checked in the validate method
        struct.validate();
      }

      public void write(org.apache.thrift.protocol.TProtocol oprot, getInfo_result struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        if (struct.success != null) {
          oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
          oprot.writeBinary(struct.success);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class getInfo_resultTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public getInfo_resultTupleScheme getScheme() {
        return new getInfo_resultTupleScheme();
      }
    }

    private static class getInfo_resultTupleScheme extends org.apache.thrift.scheme.TupleScheme<getInfo_result> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, getInfo_result struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet optionals = new java.util.BitSet();
        if (struct.isSetSuccess()) {
          optionals.set(0);
        }
        oprot.writeBitSet(optionals, 1);
        if (struct.isSetSuccess()) {
          oprot.writeBinary(struct.success);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, getInfo_result struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet incoming = iprot.readBitSet(1);
        if (incoming.get(0)) {
          struct.success = iprot.readBinary();
          struct.setSuccessIsSet(true);
        }
      }
    }

    private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
      return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
    }
  }

  public static class onPaint_args implements org.apache.thrift.TBase<onPaint_args, onPaint_args._Fields>, java.io.Serializable, Cloneable, Comparable<onPaint_args>   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("onPaint_args");

    private static final org.apache.thrift.protocol.TField BID_FIELD_DESC = new org.apache.thrift.protocol.TField("bid", org.apache.thrift.protocol.TType.I32, (short)1);
    private static final org.apache.thrift.protocol.TField POPUP_FIELD_DESC = new org.apache.thrift.protocol.TField("popup", org.apache.thrift.protocol.TType.BOOL, (short)2);
    private static final org.apache.thrift.protocol.TField DIRTY_RECTS_FIELD_DESC = new org.apache.thrift.protocol.TField("dirtyRects", org.apache.thrift.protocol.TType.STRING, (short)3);
    private static final org.apache.thrift.protocol.TField BUFFER_FIELD_DESC = new org.apache.thrift.protocol.TField("buffer", org.apache.thrift.protocol.TType.STRING, (short)4);
    private static final org.apache.thrift.protocol.TField WIDTH_FIELD_DESC = new org.apache.thrift.protocol.TField("width", org.apache.thrift.protocol.TType.I32, (short)5);
    private static final org.apache.thrift.protocol.TField HEIGHT_FIELD_DESC = new org.apache.thrift.protocol.TField("height", org.apache.thrift.protocol.TType.I32, (short)6);

    private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new onPaint_argsStandardSchemeFactory();
    private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new onPaint_argsTupleSchemeFactory();

    public int bid; // required
    public boolean popup; // required
    public @org.apache.thrift.annotation.Nullable java.nio.ByteBuffer dirtyRects; // required
    public @org.apache.thrift.annotation.Nullable java.nio.ByteBuffer buffer; // required
    public int width; // required
    public int height; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      BID((short)1, "bid"),
      POPUP((short)2, "popup"),
      DIRTY_RECTS((short)3, "dirtyRects"),
      BUFFER((short)4, "buffer"),
      WIDTH((short)5, "width"),
      HEIGHT((short)6, "height");

      private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

      static {
        for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
          byName.put(field.getFieldName(), field);
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByThriftId(int fieldId) {
        switch(fieldId) {
          case 1: // BID
            return BID;
          case 2: // POPUP
            return POPUP;
          case 3: // DIRTY_RECTS
            return DIRTY_RECTS;
          case 4: // BUFFER
            return BUFFER;
          case 5: // WIDTH
            return WIDTH;
          case 6: // HEIGHT
            return HEIGHT;
          default:
            return null;
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, throwing an exception
       * if it is not found.
       */
      public static _Fields findByThriftIdOrThrow(int fieldId) {
        _Fields fields = findByThriftId(fieldId);
        if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
        return fields;
      }

      /**
       * Find the _Fields constant that matches name, or null if its not found.
       */
      @org.apache.thrift.annotation.Nullable
      public static _Fields findByName(java.lang.String name) {
        return byName.get(name);
      }

      private final short _thriftId;
      private final java.lang.String _fieldName;

      _Fields(short thriftId, java.lang.String fieldName) {
        _thriftId = thriftId;
        _fieldName = fieldName;
      }

      public short getThriftFieldId() {
        return _thriftId;
      }

      public java.lang.String getFieldName() {
        return _fieldName;
      }
    }

    // isset id assignments
    private static final int __BID_ISSET_ID = 0;
    private static final int __POPUP_ISSET_ID = 1;
    private static final int __WIDTH_ISSET_ID = 2;
    private static final int __HEIGHT_ISSET_ID = 3;
    private byte __isset_bitfield = 0;
    public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.BID, new org.apache.thrift.meta_data.FieldMetaData("bid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
      tmpMap.put(_Fields.POPUP, new org.apache.thrift.meta_data.FieldMetaData("popup", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
      tmpMap.put(_Fields.DIRTY_RECTS, new org.apache.thrift.meta_data.FieldMetaData("dirtyRects", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING          , true)));
      tmpMap.put(_Fields.BUFFER, new org.apache.thrift.meta_data.FieldMetaData("buffer", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING          , true)));
      tmpMap.put(_Fields.WIDTH, new org.apache.thrift.meta_data.FieldMetaData("width", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
      tmpMap.put(_Fields.HEIGHT, new org.apache.thrift.meta_data.FieldMetaData("height", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
      metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(onPaint_args.class, metaDataMap);
    }

    public onPaint_args() {
    }

    public onPaint_args(
      int bid,
      boolean popup,
      java.nio.ByteBuffer dirtyRects,
      java.nio.ByteBuffer buffer,
      int width,
      int height)
    {
      this();
      this.bid = bid;
      setBidIsSet(true);
      this.popup = popup;
      setPopupIsSet(true);
      this.dirtyRects = org.apache.thrift.TBaseHelper.copyBinary(dirtyRects);
      this.buffer = org.apache.thrift.TBaseHelper.copyBinary(buffer);
      this.width = width;
      setWidthIsSet(true);
      this.height = height;
      setHeightIsSet(true);
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public onPaint_args(onPaint_args other) {
      __isset_bitfield = other.__isset_bitfield;
      this.bid = other.bid;
      this.popup = other.popup;
      if (other.isSetDirtyRects()) {
        this.dirtyRects = org.apache.thrift.TBaseHelper.copyBinary(other.dirtyRects);
      }
      if (other.isSetBuffer()) {
        this.buffer = org.apache.thrift.TBaseHelper.copyBinary(other.buffer);
      }
      this.width = other.width;
      this.height = other.height;
    }

    public onPaint_args deepCopy() {
      return new onPaint_args(this);
    }

    @Override
    public void clear() {
      setBidIsSet(false);
      this.bid = 0;
      setPopupIsSet(false);
      this.popup = false;
      this.dirtyRects = null;
      this.buffer = null;
      setWidthIsSet(false);
      this.width = 0;
      setHeightIsSet(false);
      this.height = 0;
    }

    public int getBid() {
      return this.bid;
    }

    public onPaint_args setBid(int bid) {
      this.bid = bid;
      setBidIsSet(true);
      return this;
    }

    public void unsetBid() {
      __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __BID_ISSET_ID);
    }

    /** Returns true if field bid is set (has been assigned a value) and false otherwise */
    public boolean isSetBid() {
      return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __BID_ISSET_ID);
    }

    public void setBidIsSet(boolean value) {
      __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __BID_ISSET_ID, value);
    }

    public boolean isPopup() {
      return this.popup;
    }

    public onPaint_args setPopup(boolean popup) {
      this.popup = popup;
      setPopupIsSet(true);
      return this;
    }

    public void unsetPopup() {
      __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __POPUP_ISSET_ID);
    }

    /** Returns true if field popup is set (has been assigned a value) and false otherwise */
    public boolean isSetPopup() {
      return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __POPUP_ISSET_ID);
    }

    public void setPopupIsSet(boolean value) {
      __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __POPUP_ISSET_ID, value);
    }

    public byte[] getDirtyRects() {
      setDirtyRects(org.apache.thrift.TBaseHelper.rightSize(dirtyRects));
      return dirtyRects == null ? null : dirtyRects.array();
    }

    public java.nio.ByteBuffer bufferForDirtyRects() {
      return org.apache.thrift.TBaseHelper.copyBinary(dirtyRects);
    }

    public onPaint_args setDirtyRects(byte[] dirtyRects) {
      this.dirtyRects = dirtyRects == null ? (java.nio.ByteBuffer)null     : java.nio.ByteBuffer.wrap(dirtyRects.clone());
      return this;
    }

    public onPaint_args setDirtyRects(@org.apache.thrift.annotation.Nullable java.nio.ByteBuffer dirtyRects) {
      this.dirtyRects = org.apache.thrift.TBaseHelper.copyBinary(dirtyRects);
      return this;
    }

    public void unsetDirtyRects() {
      this.dirtyRects = null;
    }

    /** Returns true if field dirtyRects is set (has been assigned a value) and false otherwise */
    public boolean isSetDirtyRects() {
      return this.dirtyRects != null;
    }

    public void setDirtyRectsIsSet(boolean value) {
      if (!value) {
        this.dirtyRects = null;
      }
    }

    public byte[] getBuffer() {
      setBuffer(org.apache.thrift.TBaseHelper.rightSize(buffer));
      return buffer == null ? null : buffer.array();
    }

    public java.nio.ByteBuffer bufferForBuffer() {
      return org.apache.thrift.TBaseHelper.copyBinary(buffer);
    }

    public onPaint_args setBuffer(byte[] buffer) {
      this.buffer = buffer == null ? (java.nio.ByteBuffer)null     : java.nio.ByteBuffer.wrap(buffer.clone());
      return this;
    }

    public onPaint_args setBuffer(@org.apache.thrift.annotation.Nullable java.nio.ByteBuffer buffer) {
      this.buffer = org.apache.thrift.TBaseHelper.copyBinary(buffer);
      return this;
    }

    public void unsetBuffer() {
      this.buffer = null;
    }

    /** Returns true if field buffer is set (has been assigned a value) and false otherwise */
    public boolean isSetBuffer() {
      return this.buffer != null;
    }

    public void setBufferIsSet(boolean value) {
      if (!value) {
        this.buffer = null;
      }
    }

    public int getWidth() {
      return this.width;
    }

    public onPaint_args setWidth(int width) {
      this.width = width;
      setWidthIsSet(true);
      return this;
    }

    public void unsetWidth() {
      __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __WIDTH_ISSET_ID);
    }

    /** Returns true if field width is set (has been assigned a value) and false otherwise */
    public boolean isSetWidth() {
      return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __WIDTH_ISSET_ID);
    }

    public void setWidthIsSet(boolean value) {
      __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __WIDTH_ISSET_ID, value);
    }

    public int getHeight() {
      return this.height;
    }

    public onPaint_args setHeight(int height) {
      this.height = height;
      setHeightIsSet(true);
      return this;
    }

    public void unsetHeight() {
      __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __HEIGHT_ISSET_ID);
    }

    /** Returns true if field height is set (has been assigned a value) and false otherwise */
    public boolean isSetHeight() {
      return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __HEIGHT_ISSET_ID);
    }

    public void setHeightIsSet(boolean value) {
      __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __HEIGHT_ISSET_ID, value);
    }

    public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
      switch (field) {
      case BID:
        if (value == null) {
          unsetBid();
        } else {
          setBid((java.lang.Integer)value);
        }
        break;

      case POPUP:
        if (value == null) {
          unsetPopup();
        } else {
          setPopup((java.lang.Boolean)value);
        }
        break;

      case DIRTY_RECTS:
        if (value == null) {
          unsetDirtyRects();
        } else {
          if (value instanceof byte[]) {
            setDirtyRects((byte[])value);
          } else {
            setDirtyRects((java.nio.ByteBuffer)value);
          }
        }
        break;

      case BUFFER:
        if (value == null) {
          unsetBuffer();
        } else {
          if (value instanceof byte[]) {
            setBuffer((byte[])value);
          } else {
            setBuffer((java.nio.ByteBuffer)value);
          }
        }
        break;

      case WIDTH:
        if (value == null) {
          unsetWidth();
        } else {
          setWidth((java.lang.Integer)value);
        }
        break;

      case HEIGHT:
        if (value == null) {
          unsetHeight();
        } else {
          setHeight((java.lang.Integer)value);
        }
        break;

      }
    }

    @org.apache.thrift.annotation.Nullable
    public java.lang.Object getFieldValue(_Fields field) {
      switch (field) {
      case BID:
        return getBid();

      case POPUP:
        return isPopup();

      case DIRTY_RECTS:
        return getDirtyRects();

      case BUFFER:
        return getBuffer();

      case WIDTH:
        return getWidth();

      case HEIGHT:
        return getHeight();

      }
      throw new java.lang.IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new java.lang.IllegalArgumentException();
      }

      switch (field) {
      case BID:
        return isSetBid();
      case POPUP:
        return isSetPopup();
      case DIRTY_RECTS:
        return isSetDirtyRects();
      case BUFFER:
        return isSetBuffer();
      case WIDTH:
        return isSetWidth();
      case HEIGHT:
        return isSetHeight();
      }
      throw new java.lang.IllegalStateException();
    }

    @Override
    public boolean equals(java.lang.Object that) {
      if (that instanceof onPaint_args)
        return this.equals((onPaint_args)that);
      return false;
    }

    public boolean equals(onPaint_args that) {
      if (that == null)
        return false;
      if (this == that)
        return true;

      boolean this_present_bid = true;
      boolean that_present_bid = true;
      if (this_present_bid || that_present_bid) {
        if (!(this_present_bid && that_present_bid))
          return false;
        if (this.bid != that.bid)
          return false;
      }

      boolean this_present_popup = true;
      boolean that_present_popup = true;
      if (this_present_popup || that_present_popup) {
        if (!(this_present_popup && that_present_popup))
          return false;
        if (this.popup != that.popup)
          return false;
      }

      boolean this_present_dirtyRects = true && this.isSetDirtyRects();
      boolean that_present_dirtyRects = true && that.isSetDirtyRects();
      if (this_present_dirtyRects || that_present_dirtyRects) {
        if (!(this_present_dirtyRects && that_present_dirtyRects))
          return false;
        if (!this.dirtyRects.equals(that.dirtyRects))
          return false;
      }

      boolean this_present_buffer = true && this.isSetBuffer();
      boolean that_present_buffer = true && that.isSetBuffer();
      if (this_present_buffer || that_present_buffer) {
        if (!(this_present_buffer && that_present_buffer))
          return false;
        if (!this.buffer.equals(that.buffer))
          return false;
      }

      boolean this_present_width = true;
      boolean that_present_width = true;
      if (this_present_width || that_present_width) {
        if (!(this_present_width && that_present_width))
          return false;
        if (this.width != that.width)
          return false;
      }

      boolean this_present_height = true;
      boolean that_present_height = true;
      if (this_present_height || that_present_height) {
        if (!(this_present_height && that_present_height))
          return false;
        if (this.height != that.height)
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = 1;

      hashCode = hashCode * 8191 + bid;

      hashCode = hashCode * 8191 + ((popup) ? 131071 : 524287);

      hashCode = hashCode * 8191 + ((isSetDirtyRects()) ? 131071 : 524287);
      if (isSetDirtyRects())
        hashCode = hashCode * 8191 + dirtyRects.hashCode();

      hashCode = hashCode * 8191 + ((isSetBuffer()) ? 131071 : 524287);
      if (isSetBuffer())
        hashCode = hashCode * 8191 + buffer.hashCode();

      hashCode = hashCode * 8191 + width;

      hashCode = hashCode * 8191 + height;

      return hashCode;
    }

    @Override
    public int compareTo(onPaint_args other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;

      lastComparison = java.lang.Boolean.compare(isSetBid(), other.isSetBid());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetBid()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.bid, other.bid);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = java.lang.Boolean.compare(isSetPopup(), other.isSetPopup());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetPopup()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.popup, other.popup);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = java.lang.Boolean.compare(isSetDirtyRects(), other.isSetDirtyRects());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetDirtyRects()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.dirtyRects, other.dirtyRects);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = java.lang.Boolean.compare(isSetBuffer(), other.isSetBuffer());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetBuffer()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.buffer, other.buffer);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = java.lang.Boolean.compare(isSetWidth(), other.isSetWidth());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetWidth()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.width, other.width);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = java.lang.Boolean.compare(isSetHeight(), other.isSetHeight());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetHeight()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.height, other.height);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      return 0;
    }

    @org.apache.thrift.annotation.Nullable
    public _Fields fieldForId(int fieldId) {
      return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
      scheme(iprot).read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
      scheme(oprot).write(oprot, this);
    }

    @Override
    public java.lang.String toString() {
      java.lang.StringBuilder sb = new java.lang.StringBuilder("onPaint_args(");
      boolean first = true;

      sb.append("bid:");
      sb.append(this.bid);
      first = false;
      if (!first) sb.append(", ");
      sb.append("popup:");
      sb.append(this.popup);
      first = false;
      if (!first) sb.append(", ");
      sb.append("dirtyRects:");
      if (this.dirtyRects == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.dirtyRects, sb);
      }
      first = false;
      if (!first) sb.append(", ");
      sb.append("buffer:");
      if (this.buffer == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.buffer, sb);
      }
      first = false;
      if (!first) sb.append(", ");
      sb.append("width:");
      sb.append(this.width);
      first = false;
      if (!first) sb.append(", ");
      sb.append("height:");
      sb.append(this.height);
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
      // check for required fields
      // check for sub-struct validity
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      try {
        write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
      try {
        // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
        __isset_bitfield = 0;
        read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private static class onPaint_argsStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public onPaint_argsStandardScheme getScheme() {
        return new onPaint_argsStandardScheme();
      }
    }

    private static class onPaint_argsStandardScheme extends org.apache.thrift.scheme.StandardScheme<onPaint_args> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, onPaint_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 1: // BID
              if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
                struct.bid = iprot.readI32();
                struct.setBidIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 2: // POPUP
              if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
                struct.popup = iprot.readBool();
                struct.setPopupIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 3: // DIRTY_RECTS
              if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
                struct.dirtyRects = iprot.readBinary();
                struct.setDirtyRectsIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 4: // BUFFER
              if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
                struct.buffer = iprot.readBinary();
                struct.setBufferIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 5: // WIDTH
              if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
                struct.width = iprot.readI32();
                struct.setWidthIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 6: // HEIGHT
              if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
                struct.height = iprot.readI32();
                struct.setHeightIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            default:
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          }
          iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        // check for required fields of primitive type, which can't be checked in the validate method
        struct.validate();
      }

      public void write(org.apache.thrift.protocol.TProtocol oprot, onPaint_args struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        oprot.writeFieldBegin(BID_FIELD_DESC);
        oprot.writeI32(struct.bid);
        oprot.writeFieldEnd();
        oprot.writeFieldBegin(POPUP_FIELD_DESC);
        oprot.writeBool(struct.popup);
        oprot.writeFieldEnd();
        if (struct.dirtyRects != null) {
          oprot.writeFieldBegin(DIRTY_RECTS_FIELD_DESC);
          oprot.writeBinary(struct.dirtyRects);
          oprot.writeFieldEnd();
        }
        if (struct.buffer != null) {
          oprot.writeFieldBegin(BUFFER_FIELD_DESC);
          oprot.writeBinary(struct.buffer);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldBegin(WIDTH_FIELD_DESC);
        oprot.writeI32(struct.width);
        oprot.writeFieldEnd();
        oprot.writeFieldBegin(HEIGHT_FIELD_DESC);
        oprot.writeI32(struct.height);
        oprot.writeFieldEnd();
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class onPaint_argsTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
      public onPaint_argsTupleScheme getScheme() {
        return new onPaint_argsTupleScheme();
      }
    }

    private static class onPaint_argsTupleScheme extends org.apache.thrift.scheme.TupleScheme<onPaint_args> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, onPaint_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet optionals = new java.util.BitSet();
        if (struct.isSetBid()) {
          optionals.set(0);
        }
        if (struct.isSetPopup()) {
          optionals.set(1);
        }
        if (struct.isSetDirtyRects()) {
          optionals.set(2);
        }
        if (struct.isSetBuffer()) {
          optionals.set(3);
        }
        if (struct.isSetWidth()) {
          optionals.set(4);
        }
        if (struct.isSetHeight()) {
          optionals.set(5);
        }
        oprot.writeBitSet(optionals, 6);
        if (struct.isSetBid()) {
          oprot.writeI32(struct.bid);
        }
        if (struct.isSetPopup()) {
          oprot.writeBool(struct.popup);
        }
        if (struct.isSetDirtyRects()) {
          oprot.writeBinary(struct.dirtyRects);
        }
        if (struct.isSetBuffer()) {
          oprot.writeBinary(struct.buffer);
        }
        if (struct.isSetWidth()) {
          oprot.writeI32(struct.width);
        }
        if (struct.isSetHeight()) {
          oprot.writeI32(struct.height);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, onPaint_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
        java.util.BitSet incoming = iprot.readBitSet(6);
        if (incoming.get(0)) {
          struct.bid = iprot.readI32();
          struct.setBidIsSet(true);
        }
        if (incoming.get(1)) {
          struct.popup = iprot.readBool();
          struct.setPopupIsSet(true);
        }
        if (incoming.get(2)) {
          struct.dirtyRects = iprot.readBinary();
          struct.setDirtyRectsIsSet(true);
        }
        if (incoming.get(3)) {
          struct.buffer = iprot.readBinary();
          struct.setBufferIsSet(true);
        }
        if (incoming.get(4)) {
          struct.width = iprot.readI32();
          struct.setWidthIsSet(true);
        }
        if (incoming.get(5)) {
          struct.height = iprot.readI32();
          struct.setHeightIsSet(true);
        }
      }
    }

    private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
      return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
    }
  }

}
