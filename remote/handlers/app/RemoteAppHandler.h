#ifndef JCEF_REMOTEAPPHANDLER_H
#define JCEF_REMOTEAPPHANDLER_H
#include "../../Utils.h"
#include "RemoteBrowserProcessHandler.h"
#include "include/cef_app.h"

class RemoteAppHandler : public CefApp {
 public:
  static RemoteAppHandler* instance();
  static void initialize(
      std::vector<std::string> switches, CefSettings settings, std::vector<std::pair<std::string, int>> schemes);

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

 private:
  std::vector<std::string> myArgs;
  CefSettings mySettings;
  std::vector<std::pair<std::string, int>> mySchemes;

  CefRefPtr<RemoteBrowserProcessHandler> myBrowserProcessHandler;

  explicit RemoteAppHandler(std::vector<std::string> switches, CefSettings settings, std::vector<std::pair<std::string, int>> schemes);

  static RemoteAppHandler * sInstance;
  IMPLEMENT_REFCOUNTING(RemoteAppHandler);
};


#endif  // JCEF_REMOTEAPPHANDLER_H
