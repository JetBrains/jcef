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
    int popup_id,
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
  return myService->exec<bool>([&](const JavaService& s){
    // TODO: support other params and return values
    Log::error("Unimplemented some params transferring");
    return s->LifeSpanHandler_OnBeforePopup(myBid, frm.serverId(), target_url.ToString(), target_frame_name.ToString(), user_gesture);
  }, false);
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  if (myBrowser == nullptr) {
    myBrowser = browser;
    Log::trace("Created native browser id=%d [bid=%d]", browser->GetIdentifier(), myBid);
    myService->exec([&](const JavaService& s){
      s->LifeSpanHandler_OnAfterCreated(myBid, browser->GetIdentifier());
    });
  } else {
    Log::trace("Created dev-tools popup id=%d [bid=%d]", browser->GetIdentifier(), myBid);
    // NOTE: don't notify LSH because it's unnecessary now:
    // dev-tool is completely independent popup-window, and we don't process events of it
  }
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  return myService->exec<bool>([&](const JavaService& s){
    return s->LifeSpanHandler_DoClose(myBid);
  }, false);
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  LNDCT();
  if (!browser)
    return;

  if (myBrowser == nullptr) {
    Log::error("OnBeforeClose: can't be 'myBrowser == nullptr', id=%d [bid=%d]", browser->GetIdentifier(), myBid);
  } else if (myBrowser->GetIdentifier() == browser->GetIdentifier()) {
    myBrowser = nullptr;
    myClientsManager->erase(myBid);
    myRoutersManager->OnBeforeClose(browser);
    myService->exec([&](const JavaService& s){
      s->LifeSpanHandler_OnBeforeClose(myBid);
    });
    Log::trace("Destroyed native browser id=%d [bid=%d]", browser->GetIdentifier(), myBid);
  } else {
    Log::trace("Closed dev-tools popup id=%d [bid=%d]", browser->GetIdentifier(), myBid);
    // NOTE: don't notify LSH because it's unnecessary now:
    // dev-tool is completely independent popup-window, and we don't process events of it
  }
}

CefRefPtr<CefBrowser> RemoteLifespanHandler::getBrowser() {
  return myBrowser;
}
