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
  LNDCT();
  myOwner.exec([&](RpcExecutor::Service s){
    // TODO: support other params and return values
    Log::error("Unimplemented some params transferring");
    s->LifeSpanHandler_OnBeforePopup(myOwner.getBid(), target_url.ToString(), target_frame_name.ToString(), user_gesture);
  });
  return false;
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  myBrowser = browser;
  myOwner.exec([&](RpcExecutor::Service s){
    s->LifeSpanHandler_OnAfterCreated(myOwner.getBid());
  });
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  myBrowser = nullptr;
  myOwner.exec([&](RpcExecutor::Service s){
    s->LifeSpanHandler_DoClose(myOwner.getBid());
  });
  return false;
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  myOwner.getRoutersManager()->OnBeforeClose(browser);
  myOwner.exec([&](RpcExecutor::Service s){
    s->LifeSpanHandler_OnBeforeClose(myOwner.getBid());
  });
}

CefRefPtr<CefBrowser> RemoteLifespanHandler::getBrowser() {
  return myBrowser;
}
