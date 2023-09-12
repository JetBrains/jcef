#ifndef IPC_JAVARENDERHANDLER_H
#define IPC_JAVARENDERHANDLER_H

#include <boost/interprocess/managed_shared_memory.hpp>

#include "../Utils.h"
#include "../gen-cpp/ClientHandlers.h"
#include "include/cef_render_handler.h"

class RemoteClientHandler;
class RemoteRenderHandler : public CefRenderHandler {
public:
 explicit RemoteRenderHandler(RemoteClientHandler & owner);
 ~RemoteRenderHandler() override;

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

  char mySharedMemName[64]{};

  boost::interprocess::managed_shared_memory::handle_t mySharedMemHandle{};
  boost::interprocess::managed_shared_memory * mySharedSegment = nullptr;
  void * mySharedMem = nullptr;
  size_t myLen = 0;

  bool _ensureSharedCapacity(size_t len);
  void _releaseSharedMem();

private:
  IMPLEMENT_REFCOUNTING(RemoteRenderHandler);
};

#endif //IPC_JAVARENDERHANDLER_H
