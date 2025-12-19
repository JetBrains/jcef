#ifndef JCEF_RPCEXECUTOR_H
#define JCEF_RPCEXECUTOR_H

#include <mutex>
#include "./gen-cpp/ClientHandlers.h"
#include "log/Log.h"

class MyBinaryProtocol;

typedef std::shared_ptr<thrift_codegen::ClientHandlersClient> JavaService;

class RpcExecutor {
 public:
  RpcExecutor(int port);
  RpcExecutor(std::string pipeName);

  void close();
  bool isClosed() { return myService == nullptr; }

  // Thread-safe RPC execution.
  template<typename T>
  T exec(std::function<T(JavaService)> rpc, T defVal) {
    std::unique_lock<std::recursive_mutex> lock(myMutex);
    if (myService == nullptr)
      return defVal;

    ExecHolder eh(*this);
    try {
      T returnVal = rpc(myService);
      return returnVal;
    } catch (apache::thrift::TException& tx) {
      onThriftException(tx);
    }
    return defVal;
  }

  void exec(std::function<void(JavaService)> rpc);

  bool isProcessing() const { return myIsProcessing; }
  Clock::time_point getProcessingStart() const { return myStartExec; }
  std::string getProcessingName() const;

 private:
  JavaService myService = nullptr;
  std::shared_ptr<apache::thrift::transport::TTransport> myTransport;
  std::shared_ptr<MyBinaryProtocol> myProtocol;
  std::recursive_mutex myMutex;

  std::chrono::steady_clock::time_point myStartExec;
  volatile bool myIsProcessing = false;
  void beforeExec();
  void afterExec();
  void onThriftException(apache::thrift::TException& tx);

  class ExecHolder {
    RpcExecutor & myExecutor;
  public:
    ExecHolder(RpcExecutor & executor) : myExecutor(executor) {
      myExecutor.beforeExec();
    }
    ~ExecHolder() {
      myExecutor.afterExec();
    }
  };
};

typedef std::unique_lock<std::recursive_mutex> Lock;

#endif  // JCEF_RPCEXECUTOR_H
