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
  bool needInvokeCallback = false;
  {
    Lock lock(myMutex);
    myService = service;
    needInvokeCallback = myIsContextInitialized;
  }
  if (myService && needInvokeCallback) {
    // Service was created after OnContextInitialized happened, so notify client immediately.
    myService->exec([&](const JavaService& s) {
      s->AppHandler_OnContextInitialized();
    });
  }
}

void RemoteBrowserProcessHandler::OnContextInitialized() {
  LNDCT();

  if (Log::isTraceEnabled()) {
    Duration dur = std::chrono::duration_cast<std::chrono::microseconds>(Clock::now() - myCreationTime);
    Log::trace("CEF context is initialized, spent %d mcs", (int)dur.count());
  }

  bool needInvokeCallback = false;
  {
    Lock lock(myMutex);
    myIsContextInitialized = true;
    needInvokeCallback = myService != nullptr;
  }
  if (needInvokeCallback)
    myService->exec([&](const JavaService& s){
      s->AppHandler_OnContextInitialized();
    });
}
