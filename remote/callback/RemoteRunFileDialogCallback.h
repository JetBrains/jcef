#ifndef JCEF_REMOTERUNFILEDIALOGCALLBACK_H
#define JCEF_REMOTERUNFILEDIALOGCALLBACK_H

#include "../RemoteObjects.h"
#include "include/cef_browser.h"

class RemoteRunFileDialogCallback : public CefRunFileDialogCallback, public RemoteJavaObject<RemoteRunFileDialogCallback> {
 public:
  explicit RemoteRunFileDialogCallback(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer);
  void OnFileDialogDismissed(const std::vector<CefString>& file_paths) override;

 private:
  IMPLEMENT_REFCOUNTING(RemoteRunFileDialogCallback);
};

#endif  // JCEF_REMOTERUNFILEDIALOGCALLBACK_H
