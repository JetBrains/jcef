#include "RemoteRenderHandler.h"
#include "RemoteClientHandler.h"

#include <iostream>

#include "../CefUtils.h"
#include "../Utils.h"
#include "../log/Log.h"

using namespace std::chrono;
using namespace thrift_codegen;
using namespace boost::interprocess;

// TODO: Optimize RemoteRenderHandler.
// Need to perform all calculations on server. Client should regularly update data on server.

const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemoteRenderHandler");

RemoteRenderHandler::RemoteRenderHandler(int bid,
                                         std::shared_ptr<RpcExecutor> service)
    : myBid(bid),
      myService(service) {}

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
    fillDummy(rect);
    Rect result;
    result.w = -1; // invalidate
    myService->exec([&](const JavaService& s){
      s->RenderHandler_GetViewRect(result, myBid);
    });
    if (result.w < 0) {
      if (doTrace)
        Log::trace("RemoteRenderHandler::GetViewRect: bid=%d, result.w = %d", myBid, result.w);
      return;
    }

    rect.x = result.x;
    rect.y = result.y;
    rect.width = result.w;
    rect.height = result.h;

    if (rect.width < 1 || rect.height < 1) {
        Log::trace("RemoteRenderHandler::GetViewRect: bid=%d, small size %d %d", myBid, rect.width, rect.height);
        fillDummy(rect);
    }
    if (doTrace)
      Log::trace("RemoteRenderHandler::GetViewRect: bid=%d, result: %d %d %d %d", myBid, rect.x, rect.y, rect.width, rect.height);
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
    fillDummy(screen_info);
    ScreenInfo result;
    result.depth = -1;// invalidate
    myService->exec([&](const JavaService& s){
      s->RenderHandler_GetScreenInfo(result, myBid);
    });
    if (result.depth == -1) {
      if (doTrace)
        Log::trace("RemoteRenderHandler::GetScreenInfo: bid=%d, result.depth == -1", myBid);
      return false;
    }

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
    if (doTrace)
      Log::trace("RemoteRenderHandler::GetScreenInfo: bid=%d, result: rc %d %d %d %d, avail %d %d %d %d", myBid, result.rect.x, result.rect.y, result.rect.w, result.rect.h, result.available_rect.x, result.available_rect.y, result.available_rect.w, result.available_rect.w);
    return true;
}

bool RemoteRenderHandler::GetScreenPoint(CefRefPtr<CefBrowser> browser,
                                   int viewX,
                                   int viewY,
                                   int& screenX,
                                   int& screenY) {
    thrift_codegen::Point result;
    result.x = INT32_MIN;// invalidate
    myService->exec([&](const JavaService& s){
      s->RenderHandler_GetScreenPoint(result, myBid, viewX, viewY);
    });
    if (result.x == INT32_MIN) {
        if (doTrace)
          Log::trace("RemoteRenderHandler::GetScreenPoint: bid=%d, result.x == INT32_MIN", myBid);
      return false;
    }

    screenX = result.x;
    screenY = result.y;
    if (doTrace)
      Log::trace("RemoteRenderHandler::GetScreenPoint: bid=%d, result: %d %d", result.x, result.y, myBid);
    return true;
}

void RemoteRenderHandler::OnPopupShow(CefRefPtr<CefBrowser> browser, bool show) {
  if (doTrace)
    Log::trace("RemoteRenderHandler::OnPopupShow: bid=%d, show=%d", myBid, show ? 1 : 0);
  myService->exec([&](const JavaService& s){
    s->RenderHandler_OnPopupShow(myBid, show);
  });
}

void RemoteRenderHandler::OnPopupSize(CefRefPtr<CefBrowser> browser,
                                      const CefRect& rect) {
  if (doTrace)
    Log::trace("RemoteRenderHandler::OnPopupSize: bid=%d, x=%d y=%d w=%d h=%d)", myBid, rect.x, rect.y, rect.width, rect.height);
  myService->exec([&](const JavaService& s) {
    Rect size;
    size.x = rect.x;
    size.y = rect.y;
    size.w = rect.width;
    size.h = rect.height;
    s->RenderHandler_OnPopupSize(myBid, size);
  });
}

//
// Debug methods
//

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
  if (doTrace)
    Log::trace("RemoteRenderHandler::OnPaint: bid=%d, width=%d height=%d, dirty rects count %d", myBid, width, height, dirtyRects.size());
  const int rasterPixCount = width * height;
  const size_t extendedRectsCount =
      dirtyRects.size() < 10 ? 10 : dirtyRects.size();
  SharedBufferManager& bufferManager =
      type == PET_POPUP ? myBufferManagerPopup : myBufferManagerPage;

  SharedBuffer* buff = bufferManager.getLockedBuffer(
      rasterPixCount * 4 + 4 * 4 * extendedRectsCount);

  if (buff == nullptr)
    return; // NOTE: error was logged in getLockedBuffer()

  if (buff->ptr() == nullptr) {
    Log::error("SharedBuffer is empty.");
    return;
  }

  ::memcpy((char*)buff->ptr(), (char*)buffer, rasterPixCount*4);

    int32_t * sharedRects = (int32_t *)buff->ptr() + rasterPixCount;
    for (const CefRect& r : dirtyRects) {
      *(sharedRects++) = r.x;
      *(sharedRects++) = r.y;
      *(sharedRects++) = r.width;
      *(sharedRects++) = r.height;
    }

    { // Draw debug
      static int drawDebug = -1;
        if (drawDebug < 0)
          drawDebug = getBoolEnv("CEF_SERVER_DRAW_DEBUG") ? 1 : 0;
        if (drawDebug > 0) {
          const int stride = width*4;
          const int th = 30;
          fillRect((unsigned char *)buff->ptr(), stride, 0, 0, th, th, 255, 0, 0, 255, width, height);
          fillRect((unsigned char *)buff->ptr(), stride, 0, width - th, th, th, 0, 255, 0, 255, width, height);
          fillRect((unsigned char *)buff->ptr(), stride, height - th, width - th, th, th, 0, 0, 255, 255, width, height);
          fillRect((unsigned char *)buff->ptr(), stride, height - th, 0, th, th, 255, 0, 255, 255, width, height);
        }
    }

    buff->unlock();

    myService->exec([&](const JavaService& s){
      s->RenderHandler_OnPaint(myBid, type != PET_VIEW, static_cast<int>(dirtyRects.size()),
                 buff->uid(), buff->handle(),
                 width, height);
    });
}

bool RemoteRenderHandler::StartDragging(CefRefPtr<CefBrowser> browser,
                                  CefRefPtr<CefDragData> drag_data,
                                  DragOperationsMask allowed_ops,
                                  int x,
                                  int y) {
    if (doTrace)
      Log::trace("RemoteRenderHandler::StartDragging: bid=%d, x=%d y=%d", myBid, x, y);
    Log::error("Unimplemented.");
    return false;
}

void RemoteRenderHandler::UpdateDragCursor(CefRefPtr<CefBrowser> browser,
                                     DragOperation operation) {
  if (doTrace)
    Log::trace("RemoteRenderHandler::UpdateDragCursor: bid=%d", myBid);
  Log::error("Unimplemented.");
}

void RemoteRenderHandler::OnImeCompositionRangeChanged(
    CefRefPtr<CefBrowser> browser,
    const CefRange& cef_selected_range,
    const RectList& cef_character_bounds) {
  if (doTrace)
    Log::trace("RemoteRenderHandler::OnImeCompositionRangeChanged: bid=%d, cef_character_bounds.size=%d", myBid, cef_character_bounds.size());
  myService->exec([&](const JavaService& s) {
    Range selected_range;
    selected_range.from = cef_selected_range.from;
    selected_range.to = cef_selected_range.to;
    std::vector<Rect> character_bounds;

    for (const auto& r : cef_character_bounds) {
      character_bounds.emplace_back();
      character_bounds.back().x = r.x;
      character_bounds.back().y = r.y;
      character_bounds.back().w = r.width;
      character_bounds.back().h = r.height;
    }
    s->RenderHandler_OnImeCompositionRangeChanged(myBid, selected_range,
                                                  character_bounds);
  });
}

void RemoteRenderHandler::OnTextSelectionChanged(
    CefRefPtr<CefBrowser> browser,
    const CefString& selected_text,
    const CefRange& cef_selected_range) {
  if (doTrace)
    Log::trace("RemoteRenderHandler::OnTextSelectionChanged: bid=%d", myBid);
  thrift_codegen::Range selected_range;
  selected_range.from = cef_selected_range.from;
  selected_range.to = cef_selected_range.to;

  myService->exec([&](const JavaService& s) {
    s->RenderHandler_OnTextSelectionChanged(myBid, selected_text.ToString(), selected_range);
  });
}
