#ifndef IPC_JAVARENDERHANDLER_H
#define IPC_JAVARENDERHANDLER_H

#include "include/cef_render_handler.h"
#include "SharedBufferManager.h"

class RemoteClientHandler;
class RemoteRenderHandler : public CefRenderHandler {
public:
 explicit RemoteRenderHandler(RemoteClientHandler & owner);

 bool GetRootScreenRect(CefRefPtr<CefBrowser> browser,
                                   CefRect &rect) override;

  void GetViewRect(CefRefPtr<CefBrowser> browser,
                           CefRect &rect) override;

  bool GetScreenInfo(CefRefPtr<CefBrowser> browser,
                             CefScreenInfo &screen_info) override;

  bool GetScreenPoint(CefRefPtr<CefBrowser> browser,
                              int viewX,
                              int viewY,
                              int &screenX,
                              int &screenY) override;

  void OnPopupShow(CefRefPtr<CefBrowser> browser, bool show) override;

  void OnPopupSize(CefRefPtr<CefBrowser> browser,
                           const CefRect &rect) override;

  void OnPaint(CefRefPtr<CefBrowser> browser,
                       PaintElementType type,
                       const RectList &dirtyRects,
                       const void *buffer,
                       int width,
                       int height) override;

  bool StartDragging(CefRefPtr<CefBrowser> browser,
                             CefRefPtr<CefDragData> drag_data,
                             DragOperationsMask allowed_ops,
                             int x,
                             int y) override;

  void UpdateDragCursor(CefRefPtr<CefBrowser> browser,
                                DragOperation operation) override;

protected:
  RemoteClientHandler & myOwner;
  SharedBufferManager myBufferManager;

private:
  IMPLEMENT_REFCOUNTING(RemoteRenderHandler);
};

#endif //IPC_JAVARENDERHANDLER_H
