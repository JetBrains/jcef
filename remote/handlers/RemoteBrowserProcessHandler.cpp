#include "RemoteBrowserProcessHandler.h"
#include "../log/Log.h"
#include "../router/MessageRoutersManager.h"

RemoteBrowserProcessHandler::RemoteBrowserProcessHandler() : myService(nullptr) {}

RemoteBrowserProcessHandler::~RemoteBrowserProcessHandler() {
  MessageRoutersManager::ClearAllConfigs();
}

void RemoteBrowserProcessHandler::OnContextInitialized() {
  LNDCT();
  auto service = myService;
  if (service)
    service->exec([&](RpcExecutor::Service s){
      s->AppHandler_OnContextInitialized();
    });
}
