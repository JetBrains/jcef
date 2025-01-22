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
    int bid,
    int handlersMask,
    const thrift_codegen::RObject& requestContextHandler)
    : myCid(cid),
      myBid(bid),
      myService(ctx->javaService()),
      myRoutersManager(ctx->routersManager()),
      myRemoteLisfespanHandler(new RemoteLifespanHandler(bid, ctx))
{
  if (handlersMask & HandlerMasks::NativeRender)
    myRemoteRenderHandler = new RemoteRenderHandler(bid, ctx->javaService());
  else {
    myRemoteRenderHandler = new DummyRenderHandler();
    Log::trace("Bid %d hasn't renderer.", bid);
  }

  if (handlersMask & HandlerMasks::Load)
    myRemoteLoadHandler = new RemoteLoadHandler(bid, ctx->javaService());

  if (handlersMask & HandlerMasks::Display)
    myRemoteDisplayHandler = new RemoteDisplayHandler(bid, ctx->javaService());

  if (handlersMask & HandlerMasks::Request)
    myRemoteRequestHandler = new RemoteRequestHandler(bid, ctx);

  if (handlersMask & HandlerMasks::Keyboard)
    myRemoteKeyboardHandler = new RemoteKeyboardHandler(bid, ctx->javaService());

  if (handlersMask & HandlerMasks::Focus)
    myRemoteFocusHandler = new RemoteFocusHandler(bid, ctx->javaService());

  if (handlersMask & HandlerMasks::ContextMenu)
    myRemoteContextMenuHandler = new RemoteContextMenuHandler(bid, ctx->javaService());

  if (handlersMask & HandlerMasks::Permission)
    myRemotePermissionHandler = new RemotePermissionHandler(bid, ctx->javaService());

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

  // TODO: Expose CefRequestContextSettings.
  CefRequestContextSettings settings;
  myRequestContext = requestContextHandler.objId < 0
          ? CefRequestContext::GetGlobalContext()
          : CefRequestContext::CreateContext(settings,new RemoteRequestContextHandler(ctx, requestContextHandler));
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