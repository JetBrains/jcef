#ifndef JCEF_REMOTEAPPHANDLER_H
#define JCEF_REMOTEAPPHANDLER_H
#include "include/cef_app.h"
#include "Utils.h"

class RemoteAppHandler : public CefApp {
 public:
  explicit RemoteAppHandler(std::shared_ptr<BackwardConnection> backwardConnection);

  void OnBeforeCommandLineProcessing(
      const CefString& process_type,
      CefRefPtr<CefCommandLine> command_line) override;
  void OnRegisterCustomSchemes(
      CefRawPtr<CefSchemeRegistrar> registrar) override;
  CefRefPtr<CefBrowserProcessHandler> GetBrowserProcessHandler() override;

 protected:
  std::shared_ptr<BackwardConnection> myBackwardConnection;

  void _onThriftException(apache::thrift::TException e);

  IMPLEMENT_REFCOUNTING(RemoteAppHandler);
};


#endif  // JCEF_REMOTEAPPHANDLER_H
