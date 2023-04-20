#ifndef JCEF_NATIVE_CLIENT_HANDLER_H_
#define JCEF_NATIVE_CLIENT_HANDLER_H_

#include "include/cef_client.h"
#include "Utils.h"

class RemoteClientHandler : public CefClient, public ConnectionUser {
public:
  explicit RemoteClientHandler(std::shared_ptr<BackwardConnection> connection, int cid, int bid);

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

  bool OnProcessMessageReceived(CefRefPtr<CefBrowser> browser,
                                CefRefPtr<CefFrame> frame,
                                CefProcessId source_process,
                                CefRefPtr<CefProcessMessage> message) override;

  int getBid() const { return myBid; }
  int getCid() const { return myCid; }

protected:
 const int myCid;
 const int myBid;

 const CefRefPtr<CefRenderHandler> myRemoteRenderHandler;
 const CefRefPtr<CefLifeSpanHandler> myRemoteLisfespanHandler;
 const CefRefPtr<CefLoadHandler> myRemoteLoadHandler;

private:
 IMPLEMENT_REFCOUNTING(RemoteClientHandler);
};

#endif  // JCEF_NATIVE_CLIENT_HANDLER_H_

