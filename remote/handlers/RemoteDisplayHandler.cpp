#include "RemoteDisplayHandler.h"
#include "RemoteClientHandler.h"
#include "../log/Log.h"

RemoteDisplayHandler::RemoteDisplayHandler(RemoteClientHandler & owner)
    : myOwner(owner) {}

void RemoteDisplayHandler::OnAddressChange(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     const CefString& url) {
  LNDCT();
  myOwner.exec([&](const RpcExecutor::Service& s){
    s->DisplayHandler_OnAddressChange(myOwner.getBid(), url.ToString());
  });
}

void RemoteDisplayHandler::OnTitleChange(CefRefPtr<CefBrowser> browser,
                   const CefString& title) {
  LNDCT();
  myOwner.exec([&](const RpcExecutor::Service& s){
    s->DisplayHandler_OnTitleChange(myOwner.getBid(), title.ToString());
  });
}

bool RemoteDisplayHandler::OnTooltip(CefRefPtr<CefBrowser> browser, CefString& text) {
  LNDCT();
  return myOwner.exec<bool>([&](const RpcExecutor::Service& s){
    return s->DisplayHandler_OnTooltip(myOwner.getBid(), text.ToString());
  }, false);
}

void RemoteDisplayHandler::OnStatusMessage(CefRefPtr<CefBrowser> browser,
                     const CefString& value) {
  LNDCT();
  myOwner.exec([&](const RpcExecutor::Service& s){
    s->DisplayHandler_OnStatusMessage(myOwner.getBid(), value.ToString());
  });
}

bool RemoteDisplayHandler::OnConsoleMessage(CefRefPtr<CefBrowser> browser,
                      cef_log_severity_t level,
                      const CefString& message,
                      const CefString& source,
                      int line) {
  LNDCT();
  return myOwner.exec<bool>([&](const RpcExecutor::Service& s){
    return s->DisplayHandler_OnConsoleMessage(myOwner.getBid(), level, message.ToString(), source.ToString(), line);
  }, false);
}
