#ifndef JCEF_REMOTEAPPHANDLER_H
#define JCEF_REMOTEAPPHANDLER_H
#include "../Utils.h"
#include "RemoteBrowserProcessHandler.h"
#include "include/cef_app.h"

class RemoteAppHandler : public CefApp {
 public:
  static RemoteAppHandler& instance();

  void setService(std::shared_ptr<RpcExecutor> service) {
    myService = service;
    myBrowserProcessHandler->setService(service);
  }
  void setArgs(const std::vector<std::string> & args) {
    myArgs.clear();
    myArgs.assign(args.begin(), args.end());
  }
  void setSettings(const std::map<std::string, std::string>& settings) {
    mySettings.clear();
    mySettings.insert(settings.begin(), settings.end());
  }

  const std::vector<std::string>& getArgs() const { return myArgs; }
  const std::map<std::string, std::string>& getSettings() const { return mySettings; }

  // Similar to jcef::ClientApp implementation.
  void OnBeforeCommandLineProcessing(
      const CefString& process_type,
      CefRefPtr<CefCommandLine> command_line) override;
  void OnRegisterCustomSchemes(
      CefRawPtr<CefSchemeRegistrar> registrar) override;

  CefRefPtr<CefBrowserProcessHandler> GetBrowserProcessHandler() override { return myBrowserProcessHandler; }

 private:
  std::vector<std::string> myArgs;
  std::map<std::string, std::string> mySettings;
  std::shared_ptr<RpcExecutor> myService;
  CefRefPtr<RemoteBrowserProcessHandler> myBrowserProcessHandler;

  explicit RemoteAppHandler();

  IMPLEMENT_REFCOUNTING(RemoteAppHandler);
};


#endif  // JCEF_REMOTEAPPHANDLER_H
