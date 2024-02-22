#ifndef JCEF_UTILS_H
#define JCEF_UTILS_H

#include <mutex>
#include "./gen-cpp/ClientHandlers.h"
#include "log/Log.h"

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

template<typename ... Args>
std::string string_format( const std::string& format, Args ... args )
{
  int size_s = std::snprintf( nullptr, 0, format.c_str(), args ... ) + 1;
  if( size_s <= 0 ){ throw std::runtime_error( "Error during formatting." ); }
  auto size = static_cast<size_t>( size_s );
  std::unique_ptr<char[]> buf( new char[ size ] );
  std::snprintf( buf.get(), size, format.c_str(), args ... );
  return std::string( buf.get(), buf.get() + size - 1 );
}

#endif  // JCEF_UTILS_H
