#include "RemoteLifespanHandler.h"
#include "RemoteClientHandler.h"

#include "../log/Log.h"

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
  LogNdc ndc("RemoteLifespanHandler::OnBeforePopup");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  try {
    // TODO: support other params
    Log::error("Unimplemented some params transferring");
    remoteService->onBeforePopup(myOwner.getBid(), target_url.ToString(), target_frame_name.ToString(), user_gesture);
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return false;
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  myBrowser = browser;

  LogNdc ndc("RemoteLifespanHandler::OnAfterCreated");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onAfterCreated(myOwner.getBid());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  myBrowser = nullptr;

  LogNdc ndc("RemoteLifespanHandler::DoClose");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  try {
    remoteService->doClose(myOwner.getBid());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return false;
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  LogNdc ndc("RemoteLifespanHandler::OnBeforeClose");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onBeforeClose(myOwner.getBid());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

CefRefPtr<CefBrowser> RemoteLifespanHandler::getBrowser() {
  return myBrowser;
}
