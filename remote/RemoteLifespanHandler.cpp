#include "RemoteLifespanHandler.h"
#include "RemoteClientHandler.h"

#include "log/Log.h"

RemoteLifespanHandler::RemoteLifespanHandler(RemoteClientHandler & owner) : myOwner(owner) {}

bool RemoteLifespanHandler::OnBeforePopup(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    const CefString& target_url,
    const CefString& target_frame_name,
    CefLifeSpanHandler::WindowOpenDisposition target_disposition,
    bool user_gesture,
    const CefPopupFeatures& popupFeatures,
    CefWindowInfo& windowInfo,
    CefRefPtr<CefClient>& client,
    CefBrowserSettings& settings,
    CefRefPtr<CefDictionaryValue>& extra_info,
    bool* no_javascript_access)
{
  auto remoteService = myOwner.getBackwardConnection()->getHandlersService();
  if (remoteService == nullptr) {
    Log::debug("OnBeforePopup, null remote service");
    return false;
  }

  try {
    // TODO: support other params
    Log::error("unimplemented RemoteLifespanHandler::OnBeforePopup");
    remoteService->onBeforePopup(myOwner.getCid(), myOwner.getBid(), "", false);
  } catch (apache::thrift::TException& tx) {
    _onThriftException(tx);
  }
  return false;
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  myBrowser = browser;

  auto remoteService = myOwner.getBackwardConnection()->getHandlersService();
  if (remoteService == nullptr) {
    Log::debug("OnAfterCreated, null remote service");
    return;
  }

  try {
    remoteService->onAfterCreated(myOwner.getCid(), myOwner.getBid());
  } catch (apache::thrift::TException& tx) {
    _onThriftException(tx);
  }
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  myBrowser = nullptr;

  auto remoteService = myOwner.getBackwardConnection()->getHandlersService();
  if (remoteService == nullptr) {
    Log::debug("DoClose, null remote service");
    return false;
  }

  try {
    remoteService->doClose(myOwner.getCid(), myOwner.getBid());
  } catch (apache::thrift::TException& tx) {
    _onThriftException(tx);
    return false;
  }
  return false;
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  auto remoteService = myOwner.getBackwardConnection()->getHandlersService();
  if (remoteService == nullptr) {
    Log::debug("OnBeforeClose, null remote service");
    return;
  }

  try {
    remoteService->onBeforeClose(myOwner.getCid(), myOwner.getBid());
  } catch (apache::thrift::TException& tx) {
    _onThriftException(tx);
  }
}

CefRefPtr<CefBrowser> RemoteLifespanHandler::getBrowser() {
  return myBrowser;
}

void RemoteLifespanHandler::_onThriftException(apache::thrift::TException e) {
  Log::debug("browser [%d], thrift exception occured: %s", myOwner.getBid(), e.what());
}
