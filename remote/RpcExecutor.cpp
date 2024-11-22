#include "RpcExecutor.h"

#include <thrift/transport/TSocket.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TTransportUtils.h>

#ifdef WIN32
#include "windows/PipeTransport.h"
#else
#include <boost/filesystem.hpp>
#endif

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

using namespace thrift_codegen;

class MyBinaryProtocol : public TBinaryProtocolT<TTransport> {
 public:
  explicit MyBinaryProtocol(const std::shared_ptr<TTransport>& trans) : TBinaryProtocolT(trans) {}

  uint32_t writeMessageBegin_virt(const std::string& name,
                                  const TMessageType messageType,
                                  const int32_t seqid) override {
    myLastMessageName = name;
    return TVirtualProtocol::writeMessageBegin_virt(name, messageType, seqid);
  }

  const std::string& getLastMessageName() const { return myLastMessageName; }

 private:
  std::string myLastMessageName = "";
};

RpcExecutor::RpcExecutor(int port) {
  myTransport = std::make_shared<TBufferedTransport>(std::make_shared<TSocket>("localhost", port));
  myProtocol = std::make_shared<MyBinaryProtocol>(myTransport);
  myService = std::make_shared<ClientHandlersClient>(myProtocol);

  myTransport->open();
  const int32_t backwardCid = myService->connect();
  Log::trace("Backward tcp connection to client established, backwardCid=%d.", backwardCid);
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
  const int32_t backwardCid = myService->connect();
  Log::trace("Backward pipe connection to client established, backwardCid=%d.", backwardCid);
}

std::string RpcExecutor::getProcessingName() const { return myProtocol->getLastMessageName(); }

void RpcExecutor::beforeExec() {
  myIsProcessing = true;
  myStartExec = Clock::now();
}

void RpcExecutor::afterExec() {
  myIsProcessing = false;
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

void RpcExecutor::exec(std::function<void(Service)> rpc) {
  Lock lock(myMutex);

  if (myService == nullptr) {
    //Log::debug("null remote service");
    return;
  }

  try {
    beforeExec();
    rpc(myService);
    afterExec();
    return;
  } catch (apache::thrift::TException& tx) {
    Log::debug("thrift exception occured: %s", tx.what());
    close();
  }
  afterExec();
}
