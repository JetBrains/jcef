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

RpcExecutor::RpcExecutor(int port) {
  myTransport = std::make_shared<TBufferedTransport>(std::make_shared<TSocket>("localhost", port));
  myService = std::make_shared<ClientHandlersClient>(std::make_shared<TBinaryProtocol>(myTransport));

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
    close();
  }
}

CommandLineArgs::CommandLineArgs(int argc, char* argv[]) {
  for (int c = 0; c < argc; ++c) {
    const char * arg = argv[c];
    if (arg == nullptr)
      continue;

    std::string str(arg);
    size_t tokenPos;
    if ((tokenPos = str.find("--port=")) != str.npos) {
      std::string val = str.substr(tokenPos + 7);
      myPort = std::stoi(val);
      myUseTcp = true;
    } else if ((tokenPos = str.find("--pipe=")) != str.npos) {
      myPathPipe = str.substr(tokenPos + 7);
    } else if ((tokenPos = str.find("--logfile=")) != str.npos) {
      myPathLogFile = str.substr(tokenPos + 10);
    } else if ((tokenPos = str.find("--params=")) != str.npos) {
      myPathParamsFile = str.substr(tokenPos + 9);
    }
  }
}