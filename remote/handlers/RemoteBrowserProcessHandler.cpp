#include "RemoteBrowserProcessHandler.h"
#include "../log/Log.h"

RemoteBrowserProcessHandler::RemoteBrowserProcessHandler(std::shared_ptr<BackwardConnection> backwardConnection)
    : RpcExecutor(backwardConnection) {
}

RemoteBrowserProcessHandler::~RemoteBrowserProcessHandler() {}

void RemoteBrowserProcessHandler::OnContextInitialized() {
  LNDCT();
  exec([&](RpcExecutor::Service s){
    s->onContextInitialized();
  });
}
