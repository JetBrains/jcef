#ifndef JCEF_REMOTEBROWSERPROCESSHANDLER_H
#define JCEF_REMOTEBROWSERPROCESSHANDLER_H

#include "../Utils.h"
#include "include/cef_browser_process_handler.h"

class RemoteBrowserProcessHandler : public CefBrowserProcessHandler {
 public:
  explicit RemoteBrowserProcessHandler();
  ~RemoteBrowserProcessHandler() override;

  // NOTES: for the current JBCefApp implementation we needs only one this method.
  void OnContextInitialized() override;

  // TODO: add IsContextInitialized, because OnContextInitialized() is called once (when
  // server starts first time) and client should be able to detect this case.

  void setService(std::shared_ptr<RpcExecutor> service) { myService = service; }

 private:
  std::shared_ptr<RpcExecutor> myService;
  IMPLEMENT_REFCOUNTING(RemoteBrowserProcessHandler);
};
#endif  // JCEF_REMOTEBROWSERPROCESSHANDLER_H
