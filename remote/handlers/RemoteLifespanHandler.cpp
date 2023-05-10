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
  myOwner.exec([&](RpcExecutor::Service s){
    // TODO: support other params and return values
    Log::error("Unimplemented some params transferring");
    s->onBeforePopup(myOwner.getBid(), target_url.ToString(), target_frame_name.ToString(), user_gesture);
  });
  return false;
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  LogNdc ndc("RemoteLifespanHandler::OnAfterCreated");
  myBrowser = browser;
  myOwner.exec([&](RpcExecutor::Service s){
    s->onAfterCreated(myOwner.getBid());
  });
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  LogNdc ndc("RemoteLifespanHandler::DoClose");
  myBrowser = nullptr;
  myOwner.exec([&](RpcExecutor::Service s){
    s->doClose(myOwner.getBid());
  });
  return false;
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  LogNdc ndc("RemoteLifespanHandler::OnBeforeClose");
  myOwner.exec([&](RpcExecutor::Service s){
    s->onBeforeClose(myOwner.getBid());
  });
}

CefRefPtr<CefBrowser> RemoteLifespanHandler::getBrowser() {
  return myBrowser;
}
