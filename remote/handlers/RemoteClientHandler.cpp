#include "RemoteClientHandler.h"
#include <log4cxx/mdc.h>
#include "../log/Log.h"
#include "RemoteDisplayHandler.h"
#include "RemoteLifespanHandler.h"
#include "RemoteLoadHandler.h"
#include "RemoteRenderHandler.h"
#include "request/RemoteRequestHandler.h"

RemoteClientHandler::RemoteClientHandler(std::shared_ptr<BackwardConnection> connection, int cid, int bid)
    : ConnectionUser(connection),
      myCid(cid),
      myBid(bid),
      myRemoteRenderHandler(new RemoteRenderHandler(*this)),
      myRemoteLisfespanHandler(new RemoteLifespanHandler(*this)),
      myRemoteLoadHandler(new RemoteLoadHandler(*this)),
      myRemoteDisplayHandler(new RemoteDisplayHandler(*this)),
      myRemoteRequestHandler(new RemoteRequestHandler(*this))
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

bool RemoteClientHandler::OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                                             CefRefPtr<CefFrame> frame,
                                             CefProcessId source_process,
                                             CefRefPtr<CefProcessMessage> message) {
    Log::error("UNIMPLEMENTED: RemoteClientHandler::OnProcessMessageReceived");
    return false;
}

