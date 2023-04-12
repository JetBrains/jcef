#include "RemoteClientHandler.h"
#include "RemoteLifespanHandler.h"
#include "RemoteRenderHandler.h"
#include "log/Log.h"

RemoteClientHandler::RemoteClientHandler(std::shared_ptr<BackwardConnection> connection, int cid, int bid)
    : myBackwardConnection(connection),
      myCid(cid),
      myBid(bid),
      myRemoteRenderHandler(new RemoteRenderHandler(*this)),
      myRemoteLisfespanHandler(new RemoteLifespanHandler(*this)) {}

CefRefPtr<CefContextMenuHandler> RemoteClientHandler::GetContextMenuHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetContextMenuHandler");
    return nullptr;
}

CefRefPtr<CefDialogHandler> RemoteClientHandler::GetDialogHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetDialogHandler");
    return nullptr;
}

CefRefPtr<CefDisplayHandler> RemoteClientHandler::GetDisplayHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetDisplayHandler");
    return nullptr;
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
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetFocusHandler");
    return nullptr;
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
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetKeyboardHandler");
    return nullptr;
}

CefRefPtr<CefLifeSpanHandler> RemoteClientHandler::GetLifeSpanHandler() {
    return myRemoteLisfespanHandler;
}

CefRefPtr<CefLoadHandler> RemoteClientHandler::GetLoadHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetLoadHandler");
    return nullptr;
}

CefRefPtr<CefPrintHandler> RemoteClientHandler::GetPrintHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetPrintHandler");
    return nullptr;
}

CefRefPtr<CefRenderHandler> RemoteClientHandler::GetRenderHandler() {
    return myRemoteRenderHandler;
}

CefRefPtr<CefRequestHandler> RemoteClientHandler::GetRequestHandler() {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::GetRequestHandler");
    return nullptr;
}

bool RemoteClientHandler::OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefProcessId source_process,
                                             CefRefPtr<CefProcessMessage> message) {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::OnProcessMessageReceived");
    return false;
}

std::shared_ptr<BackwardConnection> RemoteClientHandler::getBackwardConnection() {
    return myBackwardConnection;
}

