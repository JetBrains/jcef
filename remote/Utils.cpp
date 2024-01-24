#include "Utils.h"

#include <thrift/transport/TSocket.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TTransportUtils.h>

#include "log/Log.h"
#ifdef WIN32
#include "windows/PipeTransport.h"
#else
#include <boost/filesystem.hpp>
#endif

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

using namespace thrift_codegen;

RpcExecutor::RpcExecutor() {
  myTransport = std::make_shared<TBufferedTransport>(std::make_shared<TSocket>("localhost", 9091));
  myService = std::make_shared<ClientHandlersClient>(std::make_shared<TBinaryProtocol>(myTransport));

  myTransport->open();
  const int32_t backwardCid = myService->connect();
  Log::trace("Backward socket connection to client established, backwardCid=%d.", backwardCid);
}

RpcExecutor::RpcExecutor(std::string pipeName) {
#ifdef WIN32
  myTransport = std::make_shared<PipeTransport>("\\\\.\\pipe\\" + pipeName);
#else
  boost::filesystem::path pipePath = boost::filesystem::temp_directory_path().append(pipeName).lexically_normal();
  myTransport = std::make_shared<TSocket>(pipePath.c_str());
#endif
  myService = std::make_shared<ClientHandlersClient>(std::make_shared<TBinaryProtocol>(myTransport));

  myTransport->open();
  const int32_t backwardCid = myService->connect();
  Log::trace("Backward pipe connection to client established, backwardCid=%d.", backwardCid);
}

void RpcExecutor::close() {
  Lock lock(myMutex);

  if (myService != nullptr) {
    myService = nullptr;
    myTransport->close();
    myTransport = nullptr;
  }
}

void RpcExecutor::exec(std::function<void(Service)> rpc) {
  const Clock::time_point start = Clock::now();
  Lock lock(myMutex);

  const Clock::time_point t0 = Clock::now();
  Duration d = std::chrono::duration_cast<std::chrono::microseconds>(t0 - start);
  if ((long)d.count() > 10) {
    const char * func = rpc.target_type().name();
    Log::debug("\twait for rpc mutex %d mcs, func %s", (int)d.count(), func);
  }
  if (myService == nullptr) {
    //Log::debug("null remote service");
    return;
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
    rpc(myService);
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
}


RunInDtor::RunInDtor(std::function<void()> runnable) : myRunnable(runnable) {}

RunInDtor::~RunInDtor() {
  myRunnable();
}

