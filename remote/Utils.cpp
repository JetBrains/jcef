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
  Lock lock(myMutex);

  if (myService == nullptr) {
    //Log::debug("null remote service");
    return;
  }

  try {
    rpc(myService);
  } catch (apache::thrift::TException& tx) {
    Log::debug("thrift exception occured: %s", tx.what());
    // TODO: should we call close now ?
  }
}
