#include "RemoteBrowserProcessHandler.h"
#include "../../log/Log.h"
#include "../../router/MessageRoutersManager.h"
#include "../../RpcExecutor.h"

#ifdef LNDCT
#undef LNDCT
#define LNDCT()
#endif

RemoteBrowserProcessHandler::RemoteBrowserProcessHandler() : myService(nullptr), myCreationTime(Clock::now()) {}

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
    auto dur = std::chrono::duration_cast<std::chrono::microseconds>(Clock::now() - myCreationTime);
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

///
/// Implement this method to provide app-specific behavior when an already
/// running app is relaunched with the same CefSettings.root_cache_path value.
/// For example, activate an existing app window or create a new app window.
/// |command_line| will be read-only. Do not keep a reference to
/// |command_line| outside of this method. Return true if the relaunch is
/// handled or false for default relaunch behavior. Default behavior will
/// create a new default styled Chrome window.
///
/// To avoid cache corruption only a single app instance is allowed to run for
/// a given CefSettings.root_cache_path value. On relaunch the app checks a
/// process singleton lock and then forwards the new launch arguments to the
/// already running app process before exiting early. Client apps should
/// therefore check the CefInitialize() return value for early exit before
/// proceeding.
///
/// This method will be called on the browser process UI thread.
///
/*--cef(optional_param=current_directory)--*/
bool RemoteBrowserProcessHandler::OnAlreadyRunningAppRelaunch(
    CefRefPtr<CefCommandLine> command_line,
    const CefString& current_directory) {
  return true;
}
