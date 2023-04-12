#include "Utils.h"

#include <thrift/transport/TSocket.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TTransportUtils.h>

#include "log/Log.h"

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

using namespace thrift_codegen;

BackwardConnection::BackwardConnection() {
  myTransport = std::make_shared<TBufferedTransport>(std::make_shared<TSocket>("localhost", 9091));
  myClientHandlers = std::make_shared<ClientHandlersClient>(std::make_shared<TBinaryProtocol>(myTransport));

  myTransport->open();
  const int32_t backwardCid = myClientHandlers->connect();
  Log::debug("backward connection to client established, backwardCid=%d", backwardCid);
}

void BackwardConnection::close() {
  if (myClientHandlers != nullptr) {
    myClientHandlers = nullptr;

    myTransport->close();
    myTransport = nullptr;
  }
}
