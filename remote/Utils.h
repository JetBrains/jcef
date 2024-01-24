#ifndef JCEF_UTILS_H
#define JCEF_UTILS_H

#include <mutex>
#include "./gen-cpp/ClientHandlers.h"
#include "log/Log.h"

class RunInDtor {
 public:
  RunInDtor(std::function<void()> runnable);
  virtual ~RunInDtor();

 private:
  std::function<void()> myRunnable;
};

class RpcExecutor {
 public:
  typedef std::shared_ptr<thrift_codegen::ClientHandlersClient> Service;
  RpcExecutor();
  RpcExecutor(std::string pipeName);

  void close();
  bool isClosed() { return myService == nullptr; }

  // Thread-safe RPC execution.
  template<typename T>
  T exec(std::function<T(Service)> rpc, T defVal) {
    const Clock::time_point start = Clock::now();
    std::unique_lock<std::recursive_mutex> lock(myMutex);
    const Clock::time_point t0 = Clock::now();
    Duration d = std::chrono::duration_cast<std::chrono::microseconds>(t0 - start);
    if ((long)d.count() > 10) {
      const char * func = rpc.target_type().name();
      Log::debug("\twait for rpc mutex %d mcs, func %s", (int)d.count(), func);
    }
    if (myService == nullptr) {
      //Log::debug("null remote service");
      return defVal;
    }
    RunInDtor tmp([&](){
      const Clock::time_point end = Clock::now();
      Duration d = std::chrono::duration_cast<std::chrono::microseconds>(end - t0);
      if ((long)d.count() > 10) {
        const char * func = rpc.target_type().name();
        Log::debug("\thold rpc mutex %d mcs, func %s", (int)d.count(), func);
      }
    });
    try {
      return rpc(myService);
    } catch (apache::thrift::TException& tx) {
#ifdef WIN32
      // NOTE: tx.what() returns broken memory pointer in Windows...
      // Very strange thing, need to debug later.
      Log::debug("thrift exception occured, %p", tx.what());
#else
      Log::debug("thrift exception occured: %s", tx.what());
#endif
      // TODO: should we call close now ?
    }
    return defVal;
  }

  void exec(std::function<void(Service)> rpc);

 private:
  std::shared_ptr<thrift_codegen::ClientHandlersClient> myService = nullptr;
  std::shared_ptr<apache::thrift::transport::TTransport> myTransport;
  std::recursive_mutex myMutex;
};

typedef std::unique_lock<std::recursive_mutex> Lock;

#endif  // JCEF_UTILS_H
