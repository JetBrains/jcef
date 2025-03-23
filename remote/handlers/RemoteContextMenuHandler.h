#ifndef REMOTECONTEXTMENUHANDLER_H
#define REMOTECONTEXTMENUHANDLER_H

#include "include/cef_context_menu_handler.h"

#include <set>
#include <memory>

class RpcExecutor;

class RemoteContextMenuHandler final : public CefContextMenuHandler {
  ~RemoteContextMenuHandler() override;

 public:
  RemoteContextMenuHandler(int my_bid,
                           const std::shared_ptr<RpcExecutor>& my_service);

  void OnBeforeContextMenu(CefRefPtr<CefBrowser> browser,
                           CefRefPtr<CefFrame> frame,
                           CefRefPtr<CefContextMenuParams> params,
                           CefRefPtr<CefMenuModel> model) override;

  bool RunContextMenu(CefRefPtr<CefBrowser> browser,
                      CefRefPtr<CefFrame> frame,
                      CefRefPtr<CefContextMenuParams> params,
                      CefRefPtr<CefMenuModel> model,
                      CefRefPtr<CefRunContextMenuCallback> callback) override;

  bool OnContextMenuCommand(CefRefPtr<CefBrowser> browser,
                            CefRefPtr<CefFrame> frame,
                            CefRefPtr<CefContextMenuParams> params,
                            int command_id,
                            EventFlags event_flags) override;

  void OnContextMenuDismissed(CefRefPtr<CefBrowser> browser,
                              CefRefPtr<CefFrame> frame) override;

private:
  const int myBid;
  std::shared_ptr<RpcExecutor> myService;

  IMPLEMENT_REFCOUNTING(RemoteContextMenuHandler);
};

#endif //REMOTECONTEXTMENUHANDLER_H
