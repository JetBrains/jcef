#include "RemoteDisplayHandler.h"
#include "RemoteClientHandler.h"
#include "../log/Log.h"

RemoteDisplayHandler::RemoteDisplayHandler(RemoteClientHandler & owner)
    : myOwner(owner) {}

void RemoteDisplayHandler::OnAddressChange(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     const CefString& url) {
  LogNdc ndc("RemoteDisplayHandler::OnAddressChange");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    // TODO: support frame
    remoteService->onAddressChange(myOwner.getBid(), url.ToString());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

void RemoteDisplayHandler::OnTitleChange(CefRefPtr<CefBrowser> browser,
                   const CefString& title) {
  LogNdc ndc("RemoteDisplayHandler::OnTitleChange");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onTitleChange(myOwner.getBid(), title.ToString());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

bool RemoteDisplayHandler::OnTooltip(CefRefPtr<CefBrowser> browser, CefString& text) {
  LogNdc ndc("RemoteDisplayHandler::OnTooltip");
  auto remoteService = myOwner.getService();
  if (remoteService) {
    try {
      return remoteService->onTooltip(myOwner.getBid(), text.ToString());
    } catch (apache::thrift::TException& tx) {
      myOwner.onThriftException(tx);
    }
  }
  return false;
}

void RemoteDisplayHandler::OnStatusMessage(CefRefPtr<CefBrowser> browser,
                     const CefString& value) {
  LogNdc ndc("RemoteDisplayHandler::OnStatusMessage");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onStatusMessage(myOwner.getBid(), value.ToString());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

bool RemoteDisplayHandler::OnConsoleMessage(CefRefPtr<CefBrowser> browser,
                      cef_log_severity_t level,
                      const CefString& message,
                      const CefString& source,
                      int line) {
  LogNdc ndc("RemoteDisplayHandler::OnConsoleMessage");
  auto remoteService = myOwner.getService();
  if (remoteService) {
    try {
      return remoteService->onConsoleMessage(myOwner.getBid(), level, message.ToString(), source.ToString(), line);
    } catch (apache::thrift::TException& tx) {
      myOwner.onThriftException(tx);
    }
  }
  return false;
}
