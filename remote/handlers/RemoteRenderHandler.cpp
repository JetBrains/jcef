#include "RemoteRenderHandler.h"
#include "RemoteClientHandler.h"

#include <iostream>

#include "../CefUtils.h"
#include "../log/Log.h"

using namespace std::chrono;
using namespace thrift_codegen;
using namespace boost::interprocess;

// TODO: Optimize RemoteRenderHandler.
// Need to perform all calculations on server. Client should regularly update data on server.

// Disable logging until optimized
#ifdef LNDCT
#undef LNDCT
#define LNDCT()
#endif

RemoteRenderHandler::RemoteRenderHandler(int bid, std::shared_ptr<RpcExecutor> service) : myBid(bid), myService(service), myBufferManager(bid) {}

bool RemoteRenderHandler::GetRootScreenRect(CefRefPtr<CefBrowser> browser,
                                      CefRect& rect) {
    GetViewRect(browser, rect);
    return rect.width > 1;
}

void fillDummy(CefRect& rect) {
    rect.x = 0;
    rect.y = 0;
    rect.width = 300;
    rect.height = 200;
}

void RemoteRenderHandler::GetViewRect(CefRefPtr<CefBrowser> browser, CefRect& rect) {
    LNDCT();
    fillDummy(rect);
    Rect result;
    result.w = -1; // invalidate
    myService->exec([&](const RpcExecutor::Service& s){
      s->RenderHandler_GetViewRect(result, myBid);
    });
    if (result.w < 0) return;

    rect.x = result.x;
    rect.y = result.y;
    rect.width = result.w;
    rect.height = result.h;

    if (rect.width < 1 || rect.height < 1) {
        Log::trace("GetViewRect: small size %d %d", rect.width, rect.height);
        fillDummy(rect);
    }
    //Log::trace("GetViewRect result: %d %d %d %d", rect.x, rect.y, rect.width, rect.height);
}

void fillDummy(CefScreenInfo& screen_info) {
    screen_info.device_scale_factor = 2;
    screen_info.depth = 1;
    screen_info.depth_per_component = 1;
    screen_info.is_monochrome = false;

    screen_info.rect.x = 0;
    screen_info.rect.y = 0;
    screen_info.rect.width = 500;
    screen_info.rect.height = 700;
    screen_info.available_rect.x = 0;
    screen_info.available_rect.y = 0;
    screen_info.available_rect.width = 500;
    screen_info.available_rect.height = 700;
}

///
// Called to allow the client to fill in the CefScreenInfo object with
// appropriate values. Return true if the |screen_info| structure has been
// modified.
//
// If the screen info rectangle is left empty the rectangle from GetViewRect
// will be used. If the rectangle is still empty or invalid popups may not be
// drawn correctly.
///
/*--cef()--*/
bool RemoteRenderHandler::GetScreenInfo(CefRefPtr<CefBrowser> browser,
                                  CefScreenInfo& screen_info) {
    LNDCT();
    fillDummy(screen_info);
    ScreenInfo result;
    result.depth = -1;// invalidate
    myService->exec([&](const RpcExecutor::Service& s){
      s->RenderHandler_GetScreenInfo(result, myBid);
    });
    if (result.depth == -1) return false;

    screen_info.device_scale_factor =
        static_cast<float>(result.device_scale_factor);
    screen_info.depth = result.depth;
    screen_info.depth_per_component = result.depth_per_component;
    screen_info.is_monochrome = result.is_monochrome;

    screen_info.rect.x = result.rect.x;
    screen_info.rect.y = result.rect.y;
    screen_info.rect.width = result.rect.w;
    screen_info.rect.height = result.rect.h;
    screen_info.available_rect.x = result.available_rect.x;
    screen_info.available_rect.y = result.available_rect.y;
    screen_info.available_rect.width = result.available_rect.w;
    screen_info.available_rect.height = result.available_rect.h;
    //Log::trace("GetScreenInfo result: rc %d %d %d %d, avail %d %d %d %d", result.rect.x, result.rect.y, result.rect.w, result.rect.h, result.available_rect.x, result.available_rect.y, result.available_rect.w, result.available_rect.w);
    return true;
}

bool RemoteRenderHandler::GetScreenPoint(CefRefPtr<CefBrowser> browser,
                                   int viewX,
                                   int viewY,
                                   int& screenX,
                                   int& screenY) {
    LNDCT();
    Point result;
    result.x = INT32_MIN;// invalidate
    myService->exec([&](const RpcExecutor::Service& s){
      s->RenderHandler_GetScreenPoint(result, myBid, viewX, viewY);
    });
    if (result.x == INT32_MIN) return false;

    screenX = result.x;
    screenY = result.y;
    return true;
}

void RemoteRenderHandler::OnPopupShow(CefRefPtr<CefBrowser> browser, bool show) {
    LNDCT();
    Log::error("Unimplemented.");
}

void RemoteRenderHandler::OnPopupSize(CefRefPtr<CefBrowser> browser,
                                const CefRect& rect) {
    LNDCT();
    Log::error("Unimplemented.");
}

//
// Debug methods
//
#define DRAW_DEBUG 0

inline void fillRect(unsigned char * dst, int stride, int y, int x, int dx, int dy, int r, int g, int b, int a, int width, int height) {
    if (y >= height)
      return;
    if (x >= width)
      return;

    if (y < 0) {
      dy += y;
      y = 0;
    }
    if (y + dy >= height)
      dy = height - y;

    if (x < 0) {
      dx += x;
      x = 0;
    }
    if (x + dx >= width)
      dx = width - x;

    for (int yy = y, yEnd = y + dy; yy < yEnd; ++yy) {
        const int offset = yy*stride;
        for (int xx = x, xEnd = x + dx; xx < xEnd; ++xx) {
            dst[offset + xx*4] = a; // alpha
            dst[offset + xx*4 + 1] = r; // red
            dst[offset + xx*4 + 2] = g; // green
            dst[offset + xx*4 + 3] = b; // blue
        }
    }
}

inline void drawLineX(unsigned char * dst, int stride, int y, int x, int dx, int r, int g, int b, int a, int width, int height) {
  if (y < 0)
    return;
  if (y >= height)
    return;
  if (x >= width)
    return;

  if (x < 0) {
    dx += x;
    x = 0;
  }
  if (x + dx >= width)
    dx = width - x;

  const int offset = y*stride;
  for (int xx = x, xEnd = x + dx; xx < xEnd; ++xx) {
    dst[offset + xx*4] = a; // alpha
    dst[offset + xx*4 + 1] = r; // red
    dst[offset + xx*4 + 2] = g; // green
    dst[offset + xx*4 + 3] = b; // blue
  }
}

inline void drawLineY(unsigned char * dst, int stride, int y, int x, int dy, int r, int g, int b, int a, int width, int height) {
  if (x < 0)
    return;
  if (y >= height)
    return;
  if (x >= width)
    return;

  if (y < 0) {
    dy += y;
    y = 0;
  }
  if (y + dy >= height)
    dy = height - y;

  for (int yy = y, yEnd = y + dy; yy < yEnd; ++yy) {
    const int offset = yy*stride;
    dst[offset + x*4] = a; // alpha
    dst[offset + x*4 + 1] = r; // red
    dst[offset + x*4 + 2] = g; // green
    dst[offset + x*4 + 3] = b; // blue
  }
}

inline void drawRect(unsigned char * dst, int stride, int y, int x, int width, int height, int r, int g, int b, int a, int totalWidth, int totalHeight) {
  const int thickness = 50;
  fillRect(dst, stride, y - thickness/2, x, width, thickness, r, g, b, a, totalWidth, totalHeight);
  fillRect(dst, stride, y, x + width - thickness/2, thickness, height, r, g, b, a, totalWidth, totalHeight);
  fillRect(dst, stride, y + height - thickness/2, x, width, thickness, r, g, b, a, totalWidth, totalHeight);
  fillRect(dst, stride, y, x - thickness/2, thickness, height, r, g, b, a, totalWidth, totalHeight);
}

inline void semifillRect(unsigned char * dst, int stride, int y, int x, int width, int height, int r, int g, int b, int a, int totalWidth, int totalHeight) {
  for (int xEnd = x + width; x < xEnd; x += 2)
    drawLineY(dst, stride, y, x, height, r, g, b, a, totalWidth, totalHeight);
}

void RemoteRenderHandler::OnPaint(CefRefPtr<CefBrowser> browser,
                            PaintElementType type,
                            const RectList& dirtyRects,
                            const void* buffer,
                            int width,
                            int height) {
    const int rasterPixCount = width*height;
    const size_t extendedRectsCount = dirtyRects.size() < 10 ? 10 : dirtyRects.size();
    SharedBuffer & buff = myBufferManager.getLockedBuffer(rasterPixCount*4 + 4*4*extendedRectsCount);
    if (buff.ptr() == nullptr) {
      Log::error("SharedBuffer is empty.");
      return;
    }

    ::memcpy((char*)buff.ptr(), (char*)buffer, rasterPixCount*4);

    int32_t * sharedRects = (int32_t *)buff.ptr() + rasterPixCount;
    for (const CefRect& r : dirtyRects) {
      *(sharedRects++) = r.x;
      *(sharedRects++) = r.y;
      *(sharedRects++) = r.width;
      *(sharedRects++) = r.height;
    }

#ifdef DRAW_DEBUG
    const int stride = width*4;
    const int th = 30;
    fillRect((unsigned char *)buff.ptr(), stride, 0, 0, th, th, 255, 0, 0, 255, width, height);
    fillRect((unsigned char *)buff.ptr(), stride, 0, width - th, th, th, 0, 255, 0, 255, width, height);
    fillRect((unsigned char *)buff.ptr(), stride, height - th, width - th, th, th, 0, 0, 255, 255, width, height);
    fillRect((unsigned char *)buff.ptr(), stride, height - th, 0, th, th, 255, 0, 255, 255, width, height);
#endif //DRAW_DEBUG

    buff.unlock();

    myService->exec([&](const RpcExecutor::Service& s){
      s->RenderHandler_OnPaint(myBid, type != PET_VIEW, static_cast<int>(dirtyRects.size()),
                 buff.uid(), buff.handle(),
                 width, height);
    });
}

bool RemoteRenderHandler::StartDragging(CefRefPtr<CefBrowser> browser,
                                  CefRefPtr<CefDragData> drag_data,
                                  DragOperationsMask allowed_ops,
                                  int x,
                                  int y) {
    LNDCT();
    Log::error("Unimplemented.");
    return true;
}

void RemoteRenderHandler::UpdateDragCursor(CefRefPtr<CefBrowser> browser,
                                     DragOperation operation) {
    LNDCT();
    Log::error("Unimplemented.");
}
