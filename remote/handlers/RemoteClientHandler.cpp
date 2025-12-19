#include "RemoteClientHandler.h"

#include <sstream>

#include "../ServerHandlerContext.h"
#include "../network/RemoteRequestContextHandler.h"
#include "../network/RemoteRequestHandler.h"
#include "../router/MessageRoutersManager.h"
#include "RemoteContextMenuHandler.h"
#include "RemoteDisplayHandler.h"
#include "RemoteFocusHandler.h"
#include "RemoteKeyboardHandler.h"
#include "RemoteLifespanHandler.h"
#include "RemoteLoadHandler.h"
#include "RemoteRenderHandler.h"
#include "RemotePermissionHandler.h"

namespace {

const bool doMeasureTimes = getBoolEnv("CEF_SERVER_MEASURE_RemoteClientHandler", true);

class DummyRenderHandler : public CefRenderHandler {
 public:
  void GetViewRect(CefRefPtr<CefBrowser> browser, CefRect& rect) override {
    rect.x = 0;
    rect.y = 0;
    rect.width = 20; // TODO: try to return invalid rect.
    rect.height = 20;
  }
  void OnPaint(CefRefPtr<CefBrowser> browser,
               PaintElementType type,
               const RectList& dirtyRects,
               const void* buffer,
               int width,
               int height) override {}

 private:
  IMPLEMENT_REFCOUNTING(DummyRenderHandler);
};

}  // namespace

RemoteClientHandler::RemoteClientHandler(
    std::shared_ptr<ServerHandlerContext> ctx,
    int cid,
    int handlersMask)
    : myCid(cid), myCtx(ctx),
      myService(ctx->javaService()),
      myRoutersManager(std::make_shared<MessageRoutersManager>()),
      myRemoteLisfespanHandler(new RemoteLifespanHandler(ctx, myRoutersManager))
{
  if (handlersMask & HandlerMasks::NativeRender)
    myRemoteRenderHandler = new RemoteRenderHandler(ctx->javaService());
  else {
    myRemoteRenderHandler = new DummyRenderHandler();
  }
  addHandlers(handlersMask);
}

void RemoteClientHandler::addHandlers(int handlersMask) {
    if (!myRemoteLoadHandler && handlersMask & HandlerMasks::Load)
        myRemoteLoadHandler = new RemoteLoadHandler(myCtx->javaService());

    if (!myRemoteDisplayHandler && handlersMask & HandlerMasks::Display)
        myRemoteDisplayHandler = new RemoteDisplayHandler(myCtx->javaService());

    if (!myRemoteRequestHandler && handlersMask & HandlerMasks::Request)
        myRemoteRequestHandler = new RemoteRequestHandler(myCtx, myRoutersManager);

    if (!myRemoteKeyboardHandler && handlersMask & HandlerMasks::Keyboard)
        myRemoteKeyboardHandler = new RemoteKeyboardHandler(myCtx->javaService());

    if (!myRemoteFocusHandler && handlersMask & HandlerMasks::Focus)
        myRemoteFocusHandler = new RemoteFocusHandler(myCtx->javaService());

    if (!myRemoteContextMenuHandler && handlersMask & HandlerMasks::ContextMenu)
        myRemoteContextMenuHandler = new RemoteContextMenuHandler(myCtx->javaService());

    if (!myRemotePermissionHandler && handlersMask & HandlerMasks::Permission)
        myRemotePermissionHandler = new RemotePermissionHandler(myCtx->javaService());

    // TODO: implement remaining handlers
    //  if (handlersMask & HandlerMasks::Dialog)
    //    ;
    //  if (handlersMask & HandlerMasks::JSDialog)
    //    ;
    //  if (handlersMask & HandlerMasks::Print)
    //    ;
    //  if (handlersMask & HandlerMasks::Download)
    //    ;
    //  if (handlersMask & HandlerMasks::Drag)
    //    ;
}

void RemoteClientHandler::removeHandlers(int handlersMask) {
    if (myRemoteLoadHandler && handlersMask & HandlerMasks::Load)
        myRemoteLoadHandler = nullptr;

    if (myRemoteDisplayHandler && handlersMask & HandlerMasks::Display)
        myRemoteDisplayHandler = nullptr;

    if (myRemoteRequestHandler && handlersMask & HandlerMasks::Request)
        myRemoteRequestHandler = nullptr;

    if (myRemoteKeyboardHandler && handlersMask & HandlerMasks::Keyboard)
        myRemoteKeyboardHandler = nullptr;

    if (myRemoteFocusHandler && handlersMask & HandlerMasks::Focus)
        myRemoteFocusHandler = nullptr;

    if (myRemoteContextMenuHandler && handlersMask & HandlerMasks::ContextMenu)
        myRemoteContextMenuHandler = nullptr;

    if (myRemotePermissionHandler && handlersMask & HandlerMasks::Permission)
        myRemotePermissionHandler = nullptr;

    // TODO: implement remaining handlers
    //  if (handlersMask & HandlerMasks::Dialog)
    //    ;
    //  if (handlersMask & HandlerMasks::JSDialog)
    //    ;
    //  if (handlersMask & HandlerMasks::Print)
    //    ;
    //  if (handlersMask & HandlerMasks::Download)
    //    ;
    //  if (handlersMask & HandlerMasks::Drag)
    //    ;
}

void RemoteClientHandler::addMessageRouter(std::shared_ptr<RemoteMessageRouter> router) {
    myRoutersManager->add(router);
}

void RemoteClientHandler::removeMessageRouter(std::shared_ptr<RemoteMessageRouter> router) {
    myRoutersManager->remove(router);
}

CefRefPtr<CefContextMenuHandler> RemoteClientHandler::GetContextMenuHandler() {
    return myRemoteContextMenuHandler;
}

CefRefPtr<CefDialogHandler> RemoteClientHandler::GetDialogHandler() {
    Log::error("TODO: implement: RemoteClientHandler::GetDialogHandler");
    return nullptr;
}

CefRefPtr<CefDisplayHandler> RemoteClientHandler::GetDisplayHandler() {
    return myRemoteDisplayHandler;
}

CefRefPtr<CefDownloadHandler> RemoteClientHandler::GetDownloadHandler() {
    Log::error("TODO: implement: RemoteClientHandler::GetDownloadHandler");
    return nullptr;
}

CefRefPtr<CefDragHandler> RemoteClientHandler::GetDragHandler() {
    Log::error("TODO: implement: RemoteClientHandler::GetDragHandler");
    return nullptr;
}

CefRefPtr<CefFocusHandler> RemoteClientHandler::GetFocusHandler() {
    return myRemoteFocusHandler;
}

CefRefPtr<CefPermissionHandler> RemoteClientHandler::GetPermissionHandler() {
    return myRemotePermissionHandler;
}

CefRefPtr<CefJSDialogHandler> RemoteClientHandler::GetJSDialogHandler() {
    Log::error("TODO: implement: RemoteClientHandler::GetJSDialogHandler");
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
    Log::error("TODO: implement: RemoteClientHandler::GetPrintHandler");
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
    utils::Measurer measurer(doMeasureTimes, "ClientHandler", __FUNCTION__);
    myRoutersManager->OnProcessMessageReceived(browser, frame, source_process, message);
    return false;
}

namespace HandlerMasks {
  std::string toString(int hmask) {
    std::stringstream ss;
    ss << "Lifespan";
    if (hmask & Drag)
      ss << ", Drag";
    if (hmask & Download)
      ss << ", Download";
    if (hmask & Print)
      ss << ", Print";
    if (hmask & Keyboard)
      ss << ", Keyboard";
    if (hmask & JSDialog)
      ss << ", JSDialog";
    if (hmask & Permission)
      ss << ", Permission";
    if (hmask & Focus)
      ss << ", Focus";
    if (hmask & Display)
      ss << ", Display";
    if (hmask & Dialog)
      ss << ", Dialog";
    if (hmask & ContextMenu)
      ss << ", ContextMenu";
    if (hmask & Load)
      ss << ", Load";
    if (hmask & NativeRender)
      ss << ", NativeRender";
    if (hmask & Request)
      ss << ", Request";
    return ss.str();
  }
}