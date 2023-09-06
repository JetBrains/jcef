#ifndef JCEF_REMOTEDISPLAYHANDLER_H
#define JCEF_REMOTEDISPLAYHANDLER_H

#include "../Utils.h"
#include "include/cef_display_handler.h"

class RemoteClientHandler;
class RemoteDisplayHandler : public CefDisplayHandler {
 public:
  explicit RemoteDisplayHandler(RemoteClientHandler & owner);
  virtual ~RemoteDisplayHandler() {}

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
  RemoteClientHandler & myOwner;

 private:
  IMPLEMENT_REFCOUNTING(RemoteDisplayHandler);
};
#endif  // JCEF_REMOTEDISPLAYHANDLER_H
