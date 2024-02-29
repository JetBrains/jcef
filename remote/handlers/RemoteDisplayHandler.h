#ifndef JCEF_REMOTEDISPLAYHANDLER_H
#define JCEF_REMOTEDISPLAYHANDLER_H

#include "../Utils.h"
#include "include/cef_display_handler.h"

class RemoteClientHandler;
class RpcExecutor;

// The methods of this class will be called on the UI thread.
class RemoteDisplayHandler : public CefDisplayHandler {
 public:
  explicit RemoteDisplayHandler(int bid, std::shared_ptr<RpcExecutor> service);
  ~RemoteDisplayHandler() override {}

  void OnAddressChange(CefRefPtr<CefBrowser> browser,
                       CefRefPtr<CefFrame> frame,
                       const CefString& url) override;
  void OnTitleChange(CefRefPtr<CefBrowser> browser,
                     const CefString& title) override;
  bool OnTooltip(CefRefPtr<CefBrowser> browser, CefString& text) override;
  void OnStatusMessage(CefRefPtr<CefBrowser> browser,
                       const CefString& value) override;
  bool OnConsoleMessage(CefRefPtr<CefBrowser> browser,
                        cef_log_severity_t level,
                        const CefString& message,
                        const CefString& source,
                        int line) override;

 protected:
  const int myBid;
  std::shared_ptr<RpcExecutor> myService;

 private:
  IMPLEMENT_REFCOUNTING(RemoteDisplayHandler);
};
#endif  // JCEF_REMOTEDISPLAYHANDLER_H
