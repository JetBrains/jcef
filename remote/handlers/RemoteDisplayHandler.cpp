#include "RemoteDisplayHandler.h"
#include "RemoteClientHandler.h"
#include "../log/Log.h"

RemoteDisplayHandler::RemoteDisplayHandler(RemoteClientHandler & owner)
    : myOwner(owner) {}

void RemoteDisplayHandler::OnAddressChange(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     const CefString& url) {
  LogNdc ndc("RemoteDisplayHandler::OnAddressChange");
  myOwner.exec([&](RpcExecutor::Service s){
    s->onAddressChange(myOwner.getBid(), url.ToString());
  });
}

void RemoteDisplayHandler::OnTitleChange(CefRefPtr<CefBrowser> browser,
                   const CefString& title) {
  LogNdc ndc("RemoteDisplayHandler::OnTitleChange");
  myOwner.exec([&](RpcExecutor::Service s){
    s->onTitleChange(myOwner.getBid(), title.ToString());
  });
}

bool RemoteDisplayHandler::OnTooltip(CefRefPtr<CefBrowser> browser, CefString& text) {
  LogNdc ndc("RemoteDisplayHandler::OnTooltip");
  return myOwner.exec<bool>([&](RpcExecutor::Service s){
    return s->onTooltip(myOwner.getBid(), text.ToString());
  }, false);
}

void RemoteDisplayHandler::OnStatusMessage(CefRefPtr<CefBrowser> browser,
                     const CefString& value) {
  LogNdc ndc("RemoteDisplayHandler::OnStatusMessage");
  myOwner.exec([&](RpcExecutor::Service s){
    s->onStatusMessage(myOwner.getBid(), value.ToString());
  });
}

bool RemoteDisplayHandler::OnConsoleMessage(CefRefPtr<CefBrowser> browser,
                      cef_log_severity_t level,
                      const CefString& message,
                      const CefString& source,
                      int line) {
  LogNdc ndc("RemoteDisplayHandler::OnConsoleMessage");
  return myOwner.exec<bool>([&](RpcExecutor::Service s){
    return s->onConsoleMessage(myOwner.getBid(), level, message.ToString(), source.ToString(), line);
  }, false);
}
