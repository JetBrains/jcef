#ifndef IPC_JAVARENDERHANDLER_H
#define IPC_JAVARENDERHANDLER_H

#include "include/cef_render_handler.h"

#include "./gen-cpp/ClientHandlers.h"

class RemoteRenderHandler : public CefRenderHandler {
public:
 explicit RemoteRenderHandler(const std::shared_ptr<remote::ClientHandlersClient>& client, int bid);

 virtual bool GetRootScreenRect(CefRefPtr<CefBrowser> browser,
                                   CefRect &rect) override;

  virtual void GetViewRect(CefRefPtr<CefBrowser> browser,
                           CefRect &rect) override;

  virtual bool GetScreenInfo(CefRefPtr<CefBrowser> browser,
                             CefScreenInfo &screen_info) override;

  virtual bool GetScreenPoint(CefRefPtr<CefBrowser> browser,
                              int viewX,
                              int viewY,
                              int &screenX,
                              int &screenY) override;

  virtual void OnPopupShow(CefRefPtr<CefBrowser> browser, bool show) override;

  virtual void OnPopupSize(CefRefPtr<CefBrowser> browser,
                           const CefRect &rect) override;

  virtual void OnPaint(CefRefPtr<CefBrowser> browser,
                       PaintElementType type,
                       const RectList &dirtyRects,
                       const void *buffer,
                       int width,
                       int height) override;

  virtual bool StartDragging(CefRefPtr<CefBrowser> browser,
                             CefRefPtr<CefDragData> drag_data,
                             DragOperationsMask allowed_ops,
                             int x,
                             int y) override;

  virtual void UpdateDragCursor(CefRefPtr<CefBrowser> browser,
                                DragOperation operation) override;

protected:
  std::shared_ptr<remote::ClientHandlersClient> myClient;
  const int myBid;

  IMPLEMENT_REFCOUNTING(RemoteRenderHandler);
};

#endif //IPC_JAVARENDERHANDLER_H
