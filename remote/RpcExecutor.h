#ifndef JCEF_RPCEXECUTOR_H
#define JCEF_RPCEXECUTOR_H

#include <mutex>
#include "./gen-cpp/ClientHandlers.h"
#include "log/Log.h"

class MyBinaryProtocol;

class RpcExecutor {
 public:
  typedef std::shared_ptr<thrift_codegen::ClientHandlersClient> Service;
  RpcExecutor(int port);
  RpcExecutor(std::string pipeName);

  void close();
  bool isClosed() { return myService == nullptr; }

  // Thread-safe RPC execution.
  template<typename T>
  T exec(std::function<T(Service)> rpc, T defVal) {
    std::unique_lock<std::recursive_mutex> lock(myMutex);
    if (myService == nullptr) {
      //Log::debug("null remote service");
      return defVal;
    }
    try {
      beforeExec();
      T returnVal = rpc(myService);
      afterExec();
      return returnVal;
    } catch (apache::thrift::TException& tx) {
      Log::debug("thrift exception occured: %s", tx.what());
      close();
    }
    afterExec();
    return defVal;
  }

  void exec(std::function<void(Service)> rpc);

  bool isProcessing() const { return myIsProcessing; }
  Clock::time_point getProcessingStart() const { return myStartExec; }
  std::string getProcessingName() const;

 private:
  std::shared_ptr<thrift_codegen::ClientHandlersClient> myService = nullptr;
  std::shared_ptr<apache::thrift::transport::TTransport> myTransport;
  std::shared_ptr<MyBinaryProtocol> myProtocol;
  std::recursive_mutex myMutex;

  Clock::time_point myStartExec;
  volatile bool myIsProcessing = false;
  void beforeExec();
  void afterExec();
};

typedef std::unique_lock<std::recursive_mutex> Lock;

#endif  // JCEF_RPCEXECUTOR_H
