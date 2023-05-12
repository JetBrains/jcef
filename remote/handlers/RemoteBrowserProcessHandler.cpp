#include "RemoteBrowserProcessHandler.h"
#include "../log/Log.h"

RemoteBrowserProcessHandler::RemoteBrowserProcessHandler(std::shared_ptr<RpcExecutor> service)
    : myService(service) {
}

RemoteBrowserProcessHandler::~RemoteBrowserProcessHandler() {}

void RemoteBrowserProcessHandler::OnContextInitialized() {
  LNDCT();
  myService->exec([&](RpcExecutor::Service s){
    s->onContextInitialized();
  });
}
