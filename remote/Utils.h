#ifndef JCEF_UTILS_H
#define JCEF_UTILS_H

#include <mutex>
#include "./gen-cpp/ClientHandlers.h"
#include "log/Log.h"

class RpcExecutor {
 public:
  typedef std::shared_ptr<thrift_codegen::ClientHandlersClient> Service;
  RpcExecutor();
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
      return rpc(myService);
    } catch (apache::thrift::TException& tx) {
      Log::debug("thrift exception occured: %s", tx.what());
      close();
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

class CommandLineArgs {
 public:
  CommandLineArgs(int argc, char* argv[]);

  bool useTcp() { return myUseTcp; }
  int getPort() { return myPort; }
  std::string getPipe() { return myPathPipe; }
  std::string getLogFile() { return myPathLogFile; }
  std::string getParamsFile() { return myPathParamsFile; }

 private:
  bool myUseTcp = false;
  int myPort = -1;
  std::string myPathPipe;
  std::string myPathLogFile;
  std::string myPathParamsFile;
};

#endif  // JCEF_UTILS_H
