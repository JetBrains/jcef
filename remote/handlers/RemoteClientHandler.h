#ifndef JCEF_NATIVE_CLIENT_HANDLER_H_
#define JCEF_NATIVE_CLIENT_HANDLER_H_

#include <utility>

#include "../Utils.h"
#include "../router/MessageRoutersManager.h"
#include "include/cef_client.h"

class ServerHandler;

class RemoteClientHandler : public CefClient {
public:
 explicit RemoteClientHandler(
     std::shared_ptr<MessageRoutersManager> routersManager,
     std::shared_ptr<RpcExecutor> service,
     std::shared_ptr<RpcExecutor> serviceIO,
     int cid,
     int bid);

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

    int getBid() const { return myBid; }
    int getCid() const { return myCid; }
    std::shared_ptr<RpcExecutor> getService() { return myService; }
    std::shared_ptr<MessageRoutersManager> getRoutersManager() { return myRoutersManager; }

    // Convenience methods
    template<typename T>
    T exec(std::function<T(RpcExecutor::Service)> rpc, T defVal) {
      return myService->exec(std::move(rpc), defVal);
    }
    void exec(std::function<void(RpcExecutor::Service)> rpc) { myService->exec(std::move(rpc)); }

  private:
    const int myCid;
    const int myBid;
    std::shared_ptr<RpcExecutor> myService;
    std::shared_ptr<MessageRoutersManager> myRoutersManager;

    const CefRefPtr<CefRenderHandler> myRemoteRenderHandler;
    const CefRefPtr<CefLifeSpanHandler> myRemoteLisfespanHandler;
    const CefRefPtr<CefLoadHandler> myRemoteLoadHandler;
    const CefRefPtr<CefDisplayHandler> myRemoteDisplayHandler;
    const CefRefPtr<CefRequestHandler> myRemoteRequestHandler;
    const CefRefPtr<CefKeyboardHandler> myRemoteKeyboardHandler;
    const CefRefPtr<CefFocusHandler> myRemoteFocusHandler;

    IMPLEMENT_REFCOUNTING(RemoteClientHandler);
};

#endif  // JCEF_NATIVE_CLIENT_HANDLER_H_

