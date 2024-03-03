#include "RemoteLifespanHandler.h"
#include "RemoteClientHandler.h"

#include "../ServerHandlerContext.h"
#include "../browser/RemoteFrame.h"
#include "../browser/ClientsManager.h"
#include "../router/MessageRoutersManager.h"


RemoteLifespanHandler::RemoteLifespanHandler(
    int bid,
    std::shared_ptr<ServerHandlerContext> ctx)
    : myBid(bid), myService(ctx->javaService()), myRoutersManager(ctx->routersManager()), myClientsManager(ctx->clientsManager()) {}

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
  RemoteFrame::Holder frm(frame);
  return myService->exec<bool>([&](const RpcExecutor::Service& s){
    // TODO: support other params and return values
    Log::error("Unimplemented some params transferring");
    return s->LifeSpanHandler_OnBeforePopup(myBid, frm.get()->serverIdWithMap(), target_url.ToString(), target_frame_name.ToString(), user_gesture);
  }, false);
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  myBrowser = browser;
  Log::trace("Created native browser id=%d [bid=%d]", browser->GetIdentifier(), myBid);
  myService->exec([&](const RpcExecutor::Service& s){
    s->LifeSpanHandler_OnAfterCreated(myBid, browser->GetIdentifier());
  });
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  return myService->exec<bool>([&](const RpcExecutor::Service& s){
    return s->LifeSpanHandler_DoClose(myBid);
  }, false);
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  myBrowser = nullptr;
  myClientsManager->erase(myBid);
  myRoutersManager->OnBeforeClose(browser);
  myService->exec([&](const RpcExecutor::Service& s){
    s->LifeSpanHandler_OnBeforeClose(myBid);
  });
  Log::trace("Destroyed native browser, bid=%d", myBid);
}

CefRefPtr<CefBrowser> RemoteLifespanHandler::getBrowser() {
  return myBrowser;
}
