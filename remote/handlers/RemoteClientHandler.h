#ifndef JCEF_NATIVE_CLIENT_HANDLER_H_
#define JCEF_NATIVE_CLIENT_HANDLER_H_

#include <utility>

#include "../RpcExecutor.h"
#include "include/cef_client.h"

class RemoteMessageRouter;
class ServerHandler;
class ServerHandlerContext;
class MessageRoutersManager;
class RemoteBrowser;

class RemoteClientHandler : public CefClient {
  public:
    explicit RemoteClientHandler(std::shared_ptr<ServerHandlerContext> ctx, int cid, int handlersMask);

    void addHandlers(int handlersMask);
    void removeHandlers(int handlersMask);

    void addMessageRouter(std::shared_ptr<RemoteMessageRouter> router);
    void removeMessageRouter(std::shared_ptr<RemoteMessageRouter> router);

    CefRefPtr<CefContextMenuHandler> GetContextMenuHandler() override;
    CefRefPtr<CefDialogHandler> GetDialogHandler() override;
    CefRefPtr<CefDisplayHandler> GetDisplayHandler() override;
    CefRefPtr<CefDownloadHandler> GetDownloadHandler() override;
    CefRefPtr<CefDragHandler> GetDragHandler() override;
    CefRefPtr<CefFocusHandler> GetFocusHandler() override;
    CefRefPtr<CefPermissionHandler> GetPermissionHandler() override;
    CefRefPtr<CefJSDialogHandler> GetJSDialogHandler() override;
    CefRefPtr<CefKeyboardHandler> GetKeyboardHandler() override;
    CefRefPtr<CefLifeSpanHandler> GetLifeSpanHandler() override;
    CefRefPtr<CefLoadHandler> GetLoadHandler() override;
    CefRefPtr<CefPrintHandler> GetPrintHandler() override;
    CefRefPtr<CefRenderHandler> GetRenderHandler() override;
    CefRefPtr<CefRequestHandler> GetRequestHandler() override;

    // For MessageRouter notifications
    bool OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                                  CefRefPtr<CefFrame> frame,
                                  CefProcessId source_process,
                                  CefRefPtr<CefProcessMessage> message) override;

    // Convenience methods
    template<typename T>
    T exec(std::function<T(JavaService)> rpc, T defVal) {
      return myService->exec(std::move(rpc), defVal);
    }
    void exec(std::function<void(JavaService)> rpc) { myService->exec(std::move(rpc)); }

  private:
    const int myCid;
    const std::shared_ptr<ServerHandlerContext> myCtx;
    const std::shared_ptr<RpcExecutor> myService;
    const std::shared_ptr<MessageRoutersManager> myRoutersManager; // For MessageRouter notifications

    const CefRefPtr<CefLifeSpanHandler> myRemoteLisfespanHandler; // always presented

    CefRefPtr<CefRenderHandler> myRemoteRenderHandler;
    CefRefPtr<CefLoadHandler> myRemoteLoadHandler;
    CefRefPtr<CefDisplayHandler> myRemoteDisplayHandler;
    CefRefPtr<CefRequestHandler> myRemoteRequestHandler;
    CefRefPtr<CefKeyboardHandler> myRemoteKeyboardHandler;
    CefRefPtr<CefFocusHandler> myRemoteFocusHandler;
    CefRefPtr<CefContextMenuHandler> myRemoteContextMenuHandler;
    CefRefPtr<CefPermissionHandler> myRemotePermissionHandler;

    IMPLEMENT_REFCOUNTING(RemoteClientHandler);
};

namespace HandlerMasks {
constexpr int Request     (1 << 0);
constexpr int NativeRender(1 << 1);
constexpr int Load        (1 << 2);
constexpr int ContextMenu (1 << 4);
constexpr int Dialog      (1 << 5);
constexpr int Display     (1 << 6);
constexpr int Focus       (1 << 7);
constexpr int Permission  (1 << 8);
constexpr int JSDialog    (1 << 9);
constexpr int Keyboard    (1 << 10);
constexpr int Print       (1 << 11);
constexpr int Download    (1 << 12);
constexpr int Drag        (1 << 13);

std::string toString(int hmask);
}

#endif  // JCEF_NATIVE_CLIENT_HANDLER_H_

