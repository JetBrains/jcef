package com.jetbrains.cef.remote.thrift;

import java.util.Collections;
import java.util.Map;
import com.jetbrains.cef.remote.thrift.protocol.TMessage;
import com.jetbrains.cef.remote.thrift.protocol.TMessageType;
import com.jetbrains.cef.remote.thrift.protocol.TProtocol;
import com.jetbrains.cef.remote.thrift.protocol.TProtocolUtil;
import com.jetbrains.cef.remote.thrift.protocol.TType;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

public abstract class TBaseProcessor<I> implements TProcessor {
  private static final boolean TRACE = Utils.getBoolean("jcef.trace.java.thrift", false);
  private final I iface;
  private final Map<String, ProcessFunction<I, ? extends TBase>> processMap;

  protected TBaseProcessor(
      I iface, Map<String, ProcessFunction<I, ? extends TBase>> processFunctionMap) {
    this.iface = iface;
    this.processMap = processFunctionMap;
  }

  public Map<String, ProcessFunction<I, ? extends TBase>> getProcessMapView() {
    return Collections.unmodifiableMap(processMap);
  }

  @Override
  public void process(TProtocol in, TProtocol out) throws TException {
    TMessage msg = in.readMessageBegin();
    ProcessFunction fn = processMap.get(msg.name);
    if (fn == null) {
      CefLog.Error("fn == null!!");
      TProtocolUtil.skip(in, TType.STRUCT);
      in.readMessageEnd();
      TApplicationException x =
          new TApplicationException(
              TApplicationException.UNKNOWN_METHOD, "Invalid method name: '" + msg.name + "'");
      out.writeMessageBegin(new TMessage(msg.name, TMessageType.EXCEPTION, msg.seqid));
      x.write(out);
      out.writeMessageEnd();
      out.getTransport().flush();
    } else {
      if (TRACE)
        CefLog.Debug("\t process: seq=" + msg.seqid + ", fn=" + fn.getMethodName() + "");
      fn.process(msg.seqid, in, out, iface);
      if (TRACE)
        CefLog.Debug("\t processed seq=" + msg.seqid);
    }
  }
}
