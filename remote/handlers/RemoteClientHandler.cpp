#include "RemoteClientHandler.h"

#include <utility>
#include "../log/Log.h"
#include "../network/RemoteRequestHandler.h"
#include "RemoteDisplayHandler.h"
#include "RemoteLifespanHandler.h"
#include "RemoteLoadHandler.h"
#include "RemoteRenderHandler.h"
#include "RemoteKeyboardHandler.h"
#include "RemoteFocusHandler.h"

RemoteClientHandler::RemoteClientHandler(
    std::shared_ptr<MessageRoutersManager> routersManager,
    std::shared_ptr<RpcExecutor> service,
    std::shared_ptr<RpcExecutor> serviceIO,
    int cid,
    int bid,
    std::function<void(int)> onClosedCallback)
    : myCid(cid),
      myBid(bid),
      myService(service),
      myRoutersManager(routersManager),
      myRemoteRenderHandler(new RemoteRenderHandler(bid, service)),
      myRemoteLisfespanHandler(new RemoteLifespanHandler(bid, service, routersManager, onClosedCallback)),
      myRemoteLoadHandler(new RemoteLoadHandler(bid, service)),
      myRemoteDisplayHandler(new RemoteDisplayHandler(bid, service)),
      myRemoteRequestHandler(new RemoteRequestHandler(bid, service, serviceIO, routersManager)),
      myRemoteKeyboardHandler(new RemoteKeyboardHandler(bid, service)),
      myRemoteFocusHandler(new RemoteFocusHandler(bid, service))
{}

CefRefPtr<CefContextMenuHandler> RemoteClientHandler::GetContextMenuHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetContextMenuHandler");
    return nullptr;
}

CefRefPtr<CefDialogHandler> RemoteClientHandler::GetDialogHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetDialogHandler");
    return nullptr;
}

CefRefPtr<CefDisplayHandler> RemoteClientHandler::GetDisplayHandler() {
    return myRemoteDisplayHandler;
}

CefRefPtr<CefDownloadHandler> RemoteClientHandler::GetDownloadHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetDownloadHandler");
    return nullptr;
}

CefRefPtr<CefDragHandler> RemoteClientHandler::GetDragHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetDragHandler");
    return nullptr;
}

CefRefPtr<CefFocusHandler> RemoteClientHandler::GetFocusHandler() {
    return myRemoteFocusHandler;
}

CefRefPtr<CefPermissionHandler> RemoteClientHandler::GetPermissionHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetPermissionHandler");
    return nullptr;
}

CefRefPtr<CefJSDialogHandler> RemoteClientHandler::GetJSDialogHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetJSDialogHandler");
    return nullptr;
}

CefRefPtr<CefKeyboardHandler> RemoteClientHandler::GetKeyboardHandler() {
    return myRemoteKeyboardHandler;
}

CefRefPtr<CefLifeSpanHandler> RemoteClientHandler::GetLifeSpanHandler() {
    return myRemoteLisfespanHandler;
}

CefRefPtr<CefLoadHandler> RemoteClientHandler::GetLoadHandler() {
    return myRemoteLoadHandler;
}

CefRefPtr<CefPrintHandler> RemoteClientHandler::GetPrintHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetPrintHandler");
    return nullptr;
}

CefRefPtr<CefRenderHandler> RemoteClientHandler::GetRenderHandler() {
    return myRemoteRenderHandler;
}

CefRefPtr<CefRequestHandler> RemoteClientHandler::GetRequestHandler() {
    return myRemoteRequestHandler;
}

///
/// Called when a new message is received from a different process. Return
/// true if the message was handled or false otherwise.  It is safe to keep a
/// reference to |message| outside of this callback.
///
bool RemoteClientHandler::OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefProcessId source_process,
                                             CefRefPtr<CefProcessMessage> message) {
    LNDCT();
    myRoutersManager->OnProcessMessageReceived(browser, frame, source_process, message);
    return false;
}

CefRefPtr<CefBrowser> RemoteClientHandler::getCefBrowser() {
  return ((RemoteLifespanHandler *)(myRemoteLisfespanHandler.get()))->getBrowser();
}

void RemoteClientHandler::closeBrowser() {
  if (myIsClosing)
    return;

  Log::trace("Scheduled closing native browser, bid=%d", myBid);
  myIsClosing = true;
  RemoteLifespanHandler * rlf = (RemoteLifespanHandler *)(myRemoteLisfespanHandler.get());
  auto browser = rlf->getBrowser();
  if (browser != nullptr)
    browser->GetHost()->CloseBrowser(true);
}