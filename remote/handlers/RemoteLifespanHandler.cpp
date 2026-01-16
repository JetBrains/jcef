#include "RemoteLifespanHandler.h"
#include "RemoteClientHandler.h"

#include "../ServerHandlerContext.h"
#include "../browser/RemoteFrame.h"
#include "../browser/RemoteBrowser.h"
#include "../router/MessageRoutersManager.h"

namespace {
const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemoteLifespanHandler");
}

RemoteLifespanHandler::RemoteLifespanHandler(std::shared_ptr<ServerHandlerContext> ctx, std::shared_ptr<MessageRoutersManager> routersManager)
    : myService(ctx->javaService()), myRoutersManager(routersManager) {}

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
  FIND_BID_OR_RETURN_VAL(false);
  if (doTrace)
    Log::trace("RemoteLifespanHandler::OnBeforePopup: bid=%d", bid);
  RemoteFrame::Holder frm(frame);
  return myService->exec<bool>([&](const JavaService& s){
    // TODO: support other params and return values
    Log::error("RemoteLifespanHandler: unimplemented some params transferring");
    return s->LifeSpanHandler_OnBeforePopup(bid, frm.serverId(), target_url.ToString(), target_frame_name.ToString(), user_gesture);
  }, false);
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  if (doTrace)
    Log::trace("RemoteLifespanHandler::OnAfterCreated: start for browser with cef-identifier=%d", browser->GetIdentifier());
  int bid = -1;
  {
    std::unique_lock lock(myCreatingMutex);
    if (myCreatingBids.empty()) {
      Log::trace("RemoteLifespanHandler: created dev-tools popup with identifier=%d", browser->GetIdentifier());
      // NOTE: don't notify LSH because it's unnecessary now:
      // dev-tool is completely independent popup-window, and we don't process events of it
      return;
    }
    bid = myCreatingBids.front();
    myCreatingBids.pop_front();
  }

   std::shared_ptr<RemoteBrowser> remoteBrowser = RemoteBrowser::find(bid);
   if (!remoteBrowser) {
     Log::error("RemoteLifespanHandler: OnAfterCreated: can't find RemoteBrowser by bid=%d", bid);
     return;
   }

  if (remoteBrowser->getCefBrowser())
    Log::error("RemoteLifespanHandler: OnAfterCreated: getCefBrowser != null, bid=%d", bid);

   remoteBrowser->setCefBrowser(browser);
   RemoteBrowser::linkCefBrowser(browser, remoteBrowser);

   Log::trace("RemoteLifespanHandler: OnAfterCreated: created native CefBrowser with identifier=%d [bid=%d]", browser->GetIdentifier(), bid);
   myService->exec([&](const JavaService& s){
     s->LifeSpanHandler_OnAfterCreated(bid, browser->GetIdentifier());
   });
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  FIND_BID_OR_RETURN_VAL(false);
  return myService->exec<bool>([&](const JavaService& s){
    return s->LifeSpanHandler_DoClose(bid);
  }, false);
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  const auto & rb = RemoteBrowser::findByCefBrowser(browser);
  if (!rb) {
    Log::trace("RemoteLifespanHandler: OnBeforeClose: closed dev-tools popup with identifier=%d", browser->GetIdentifier());
    // NOTE: don't notify LSH because it's unnecessary now:
    // dev-tool is completely independent popup-window, and we don't process events of it
    return;
  }

  myRoutersManager->OnBeforeClose(browser);
  myService->exec([&](const JavaService& s){
    s->LifeSpanHandler_OnBeforeClose(rb->getBid());
  });
  rb->onBeforeClose();
  RemoteBrowser::unlinkCefBrowser(browser);
  Log::trace("RemoteLifespanHandler: OnBeforeClose: destroyed native CefBrowser with identifier=%d [bid=%d]", browser->GetIdentifier(), rb->getBid());
}

void RemoteLifespanHandler::addCreating(int bid) {
  std::unique_lock lock(myCreatingMutex);
  myCreatingBids.push_back(bid);
}

void RemoteLifespanHandler::removeCreating(int bid) {
  std::unique_lock lock(myCreatingMutex);
  myCreatingBids.remove(bid);
}
