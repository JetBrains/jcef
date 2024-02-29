#include "RemoteBrowserProcessHandler.h"
#include "../../log/Log.h"
#include "../../router/MessageRoutersManager.h"
#include "../../RpcExecutor.h"

#ifdef LNDCT
#undef LNDCT
#define LNDCT()
#endif

RemoteBrowserProcessHandler::RemoteBrowserProcessHandler() : myService(nullptr), myCreationTime(Clock::now()) {}

RemoteBrowserProcessHandler::~RemoteBrowserProcessHandler() {
  MessageRoutersManager::ClearAllConfigs();
}

void RemoteBrowserProcessHandler::setService(std::shared_ptr<RpcExecutor> service) {
  Lock lock(myMutex);
  myService = service;
  if (myService && myIsContextInitialized) {
    // Service was created after OnContextInitialized happened, so notify client immediately.
    myService->exec([&](const RpcExecutor::Service& s) {
      s->AppHandler_OnContextInitialized();
    });
  }
}

void RemoteBrowserProcessHandler::OnContextInitialized() {
  LNDCT();

  if (Log::isTraceEnabled()) {
    Duration dur = std::chrono::duration_cast<std::chrono::microseconds>(Clock::now() - myCreationTime);
    Log::trace("Native CEF context initialization spent %d ms.", (int)dur.count()/1000);
  }

  Lock lock(myMutex);
  myIsContextInitialized = true;
  if (myService)
    myService->exec([&](const RpcExecutor::Service& s){
      s->AppHandler_OnContextInitialized();
    });
}
