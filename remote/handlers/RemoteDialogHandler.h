#ifndef JCEF_REMOTEDIALOGHANDLER_H
#define JCEF_REMOTEDIALOGHANDLER_H

#include "include/cef_dialog_handler.h"

class RpcExecutor;

class RemoteDialogHandler : public CefDialogHandler {
 public:
  explicit RemoteDialogHandler(std::shared_ptr<RpcExecutor> service);

  bool OnFileDialog(CefRefPtr<CefBrowser> browser,
                    FileDialogMode mode,
                    const CefString& title,
                    const CefString& default_file_path,
                    const std::vector<CefString>& accept_filters,
                    const std::vector<CefString>& accept_extensions,
                    const std::vector<CefString>& accept_descriptions,
                    CefRefPtr<CefFileDialogCallback> callback) override;

 private:
  std::shared_ptr<RpcExecutor> myService;

  IMPLEMENT_REFCOUNTING(RemoteDialogHandler);
};

#endif  // JCEF_REMOTEDIALOGHANDLER_H