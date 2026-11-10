#ifndef JCEF_REMOTEJSDIALOGHANDLER_H
#define JCEF_REMOTEJSDIALOGHANDLER_H

#include "include/cef_jsdialog_handler.h"

class RpcExecutor;

class RemoteJSDialogHandler : public CefJSDialogHandler {
 public:
  explicit RemoteJSDialogHandler(std::shared_ptr<RpcExecutor> service);

  bool OnJSDialog(CefRefPtr<CefBrowser> browser,
                  const CefString& origin_url,
                  JSDialogType dialog_type,
                  const CefString& message_text,
                  const CefString& default_prompt_text,
                  CefRefPtr<CefJSDialogCallback> callback,
                  bool& suppress_message) override;
  bool OnBeforeUnloadDialog(CefRefPtr<CefBrowser> browser,
                            const CefString& message_text,
                            bool is_reload,
                            CefRefPtr<CefJSDialogCallback> callback) override;
  void OnResetDialogState(CefRefPtr<CefBrowser> browser) override;
  void OnDialogClosed(CefRefPtr<CefBrowser> browser) override;

 private:
  std::shared_ptr<RpcExecutor> myService;

  IMPLEMENT_REFCOUNTING(RemoteJSDialogHandler);
};

#endif  // JCEF_REMOTEJSDIALOGHANDLER_H