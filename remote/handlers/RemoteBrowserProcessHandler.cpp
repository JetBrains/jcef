#include "RemoteBrowserProcessHandler.h"
#include "../log/Log.h"
#include "../router/MessageRoutersManager.h"

RemoteBrowserProcessHandler::RemoteBrowserProcessHandler(std::shared_ptr<RpcExecutor> service)
    : myService(service) {
}

RemoteBrowserProcessHandler::~RemoteBrowserProcessHandler() {
  MessageRoutersManager::ClearAllConfigs();
}

void RemoteBrowserProcessHandler::OnContextInitialized() {
  LNDCT();
  myService->exec([&](RpcExecutor::Service s){
    s->AppHandler_OnContextInitialized();
  });
}
