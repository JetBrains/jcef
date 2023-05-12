#ifndef JCEF_REMOTEAPPHANDLER_H
#define JCEF_REMOTEAPPHANDLER_H
#include "../Utils.h"
#include "RemoteBrowserProcessHandler.h"
#include "include/cef_app.h"

class RemoteAppHandler : public CefApp {
 public:
  explicit RemoteAppHandler(
      std::shared_ptr<RpcExecutor> service,
      const std::vector<std::string> & args,
      const std::map<std::string, std::string>& settings);

  // Similar to jcef::ClientApp implementation.
  void OnBeforeCommandLineProcessing(
      const CefString& process_type,
      CefRefPtr<CefCommandLine> command_line) override;
  void OnRegisterCustomSchemes(
      CefRawPtr<CefSchemeRegistrar> registrar) override;
  CefRefPtr<CefBrowserProcessHandler> GetBrowserProcessHandler() override;

 private:
  const std::vector<std::string> myArgs;
  const std::map<std::string, std::string> mySettings;
  std::shared_ptr<RpcExecutor> myService;
  CefRefPtr<CefBrowserProcessHandler> myBrowserProcessHandler;

  IMPLEMENT_REFCOUNTING(RemoteAppHandler);
};


#endif  // JCEF_REMOTEAPPHANDLER_H
