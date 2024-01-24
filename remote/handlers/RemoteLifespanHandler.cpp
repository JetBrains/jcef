#include "RemoteLifespanHandler.h"
#include "RemoteClientHandler.h"

#include "../log/Log.h"

RemoteLifespanHandler::RemoteLifespanHandler(
    int bid,
    std::shared_ptr<RpcExecutor> service,
    std::shared_ptr<MessageRoutersManager> routersManager)
    : myBid(bid), myService(service), myRoutersManager(routersManager) {}

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
  //LNDCT();
  return myService->exec<bool>([&](const RpcExecutor::Service& s){
    // TODO: support other params and return values
    Log::error("Unimplemented some params transferring");
    return s->LifeSpanHandler_OnBeforePopup(myBid, target_url.ToString(), target_frame_name.ToString(), user_gesture);
  }, false);
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  myBrowser = browser;
  Log::trace("Created native browser id=%d [bid=%d]", browser->GetIdentifier(), myBid);
  myService->exec([&](const RpcExecutor::Service& s){
    s->LifeSpanHandler_OnAfterCreated(myBid);
  });
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  myBrowser = nullptr;
  return myService->exec<bool>([&](const RpcExecutor::Service& s){
    return s->LifeSpanHandler_DoClose(myBid);
  }, false);
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  myRoutersManager->OnBeforeClose(browser);
  myService->exec([&](const RpcExecutor::Service& s){
    s->LifeSpanHandler_OnBeforeClose(myBid);
  });
}

CefRefPtr<CefBrowser> RemoteLifespanHandler::getBrowser() {
  return myBrowser;
}
