#ifndef JCEF_REMOTEAPPHANDLER_H
#define JCEF_REMOTEAPPHANDLER_H
#include "../../Utils.h"
#include "RemoteBrowserProcessHandler.h"
#include "include/cef_app.h"

class RemoteAppHandler : public CefApp {
 public:
  explicit RemoteAppHandler(std::vector<std::string> switches, CefSettings settings, std::vector<std::pair<std::string, int>> schemes);

  void setService(std::shared_ptr<RpcExecutor> service) {
    myBrowserProcessHandler->setService(service);
  }

  // Similar to jcef::ClientApp implementation.
  void OnBeforeCommandLineProcessing(
      const CefString& process_type,
      CefRefPtr<CefCommandLine> command_line) override;
  void OnRegisterCustomSchemes(
      CefRawPtr<CefSchemeRegistrar> registrar) override;

  CefRefPtr<CefBrowserProcessHandler> GetBrowserProcessHandler() override { return myBrowserProcessHandler; }

  const CefSettings & getCefSettings() const { return mySettings; }
  std::string getRootPath() const;

 private:
  std::vector<std::string> myArgs;
  CefSettings mySettings;
  std::vector<std::pair<std::string, int>> mySchemes;

  CefRefPtr<RemoteBrowserProcessHandler> myBrowserProcessHandler;

  IMPLEMENT_REFCOUNTING(RemoteAppHandler);
};


#endif  // JCEF_REMOTEAPPHANDLER_H
