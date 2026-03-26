#ifndef JCEF_REMOTEBROWSERPROCESSHANDLER_H
#define JCEF_REMOTEBROWSERPROCESSHANDLER_H

#include <utility>
#include <mutex>
#include <chrono>

#include "../../Utils.h"
#include "include/cef_browser_process_handler.h"

class RpcExecutor;

class RemoteBrowserProcessHandler : public CefBrowserProcessHandler {
 public:
  explicit RemoteBrowserProcessHandler();

  // NOTES: for the current JBCefApp implementation we needs only one this method.
  void OnContextInitialized() override;

  bool OnAlreadyRunningAppRelaunch(CefRefPtr<CefCommandLine> command_line, const CefString& current_directory) override;

  // TODO: add IsContextInitialized, because OnContextInitialized() is called once (when
  // server starts first time) and client should be able to detect this case.

  void setService(std::shared_ptr<RpcExecutor> service);

 private:
  std::shared_ptr<RpcExecutor> myService;
  std::recursive_mutex myMutex;
  const std::chrono::steady_clock::time_point myCreationTime; // just for logging
  bool myIsContextInitialized = false;
  bool myIsCallbackInvoked = false;
  IMPLEMENT_REFCOUNTING(RemoteBrowserProcessHandler);
};
#endif  // JCEF_REMOTEBROWSERPROCESSHANDLER_H
