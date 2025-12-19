#include "RpcExecutor.h"

#include <thrift/transport/TSocket.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TTransportUtils.h>

#include "Utils.h"
#include "DebugInfo.h"

#ifdef WIN32
#include "windows/PipeTransport.h"
#else
#include <boost/filesystem.hpp>
#endif

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

using namespace thrift_codegen;

namespace {
#ifndef NDEBUG
const bool doMeasureTimes = getBoolEnv("CEF_SERVER_MEASURE_RpcExecutor", true);
#else
const bool doMeasureTimes = getBoolEnv("CEF_SERVER_MEASURE_RpcExecutor", false);
#endif
const bool doTraceAll = getBoolEnv("CEF_SERVER_TRACE_RpcExecutor");
}

class MyBinaryProtocol : public TBinaryProtocolT<TTransport> {
public:
  explicit MyBinaryProtocol(const std::shared_ptr<TTransport>& trans) : TBinaryProtocolT(trans) {}

  uint32_t writeMessageBegin_virt(const std::string& name,
                                  const TMessageType messageType,
                                  const int32_t seqid) override {
    myLastMessageName = name;
    if (doTraceAll && Log::isTraceEnabled())
      Log::trace("RpcExecutor: exec '%s'", name.c_str());

    return TVirtualProtocol::writeMessageBegin_virt(name, messageType, seqid);
  }

  const std::string& getLastMessageName() const { return myLastMessageName; }

private:
  std::string myLastMessageName = "";
};

RpcExecutor::RpcExecutor(int port) {
  myTransport = std::make_shared<TBufferedTransport>(std::make_shared<TSocket>("127.0.0.1", port));
  myProtocol = std::make_shared<MyBinaryProtocol>(myTransport);
  myService = std::make_shared<ClientHandlersClient>(myProtocol);

  myTransport->open();
}

RpcExecutor::RpcExecutor(std::string pipeName) {
#ifdef WIN32
  myTransport = std::make_shared<PipeTransport>("\\\\.\\pipe\\" + pipeName);
#else
  myTransport = std::make_shared<TSocket>(pipeName.c_str());
#endif
  myProtocol = std::make_shared<MyBinaryProtocol>(myTransport);
  myService = std::make_shared<ClientHandlersClient>(myProtocol);

  myTransport->open();
}

std::string RpcExecutor::getProcessingName() const { return myProtocol->getLastMessageName(); }

void RpcExecutor::beforeExec() {
  myIsProcessing = true;
  myStartExec = std::chrono::steady_clock::now();
}

void RpcExecutor::afterExec() {
  myIsProcessing = false;
  if (doMeasureTimes)
    DebugInfo::addMeasure(
      "RpcExecutor." + myProtocol->getLastMessageName(),
      std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now() - myStartExec).count()
    );
}

void RpcExecutor::close() {
  Lock lock(myMutex);

  if (myService != nullptr) {
    myService = nullptr;
    try {
      myTransport->close();
    } catch (const TException& e) {
      Log::error("Exception during rpc-executor transport closing, err: %s", e.what());
    }
    myTransport = nullptr;
  }
}

void RpcExecutor::exec(std::function<void(JavaService)> rpc) {
  Lock lock(myMutex);

  if (myService == nullptr) {
    if (doTraceAll && Log::isTraceEnabled())
      Log::trace("RpcExecutor: null remote service");
    return;
  }

  ExecHolder eh(*this);
  try {
    rpc(myService);
  } catch (apache::thrift::TException& tx) {
    onThriftException(tx);
  }
}

void RpcExecutor::onThriftException(apache::thrift::TException& tx) {
  if (Log::isTraceEnabled()) {
    Log::trace("RpcExecutor: thrift exception occurred: %s", tx.what());
    Log::trace("RpcExecutor: name of executed rpc: %s", getProcessingName().c_str());
  }
  close();
}
