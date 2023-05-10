#ifndef JCEF_REMOTEBROWSERPROCESSHANDLER_H
#define JCEF_REMOTEBROWSERPROCESSHANDLER_H

#include "../Utils.h"
#include "include/cef_browser_process_handler.h"

class RemoteBrowserProcessHandler : public CefBrowserProcessHandler, RpcExecutor {
 public:
  explicit RemoteBrowserProcessHandler(std::shared_ptr<BackwardConnection> backwardConnection);
  virtual ~RemoteBrowserProcessHandler();

  // NOTES: for the current JBCefApp implementation we needs only one this method.
  void OnContextInitialized() override;

 private:
  IMPLEMENT_REFCOUNTING(RemoteBrowserProcessHandler);
};
#endif  // JCEF_REMOTEBROWSERPROCESSHANDLER_H
