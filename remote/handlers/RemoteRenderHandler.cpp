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
#define LNDCT()

RemoteRenderHandler::RemoteRenderHandler(RemoteClientHandler & owner) : myOwner(owner) {
    std::sprintf(mySharedMemName, "CefSharedRasterC%dB%d", myOwner.getCid(), myOwner.getBid());
}

RemoteRenderHandler::~RemoteRenderHandler() {
  _releaseSharedMem();
}

void RemoteRenderHandler::_releaseSharedMem() {
  if (mySharedSegment != nullptr) {
    mySharedSegment->deallocate(mySharedMem);
    delete mySharedSegment;

    mySharedSegment = nullptr;
    mySharedMem = nullptr;
    myLen = 0;
  }
  shared_memory_object::remove(mySharedMemName);
}

bool RemoteRenderHandler::_ensureSharedCapacity(int len) {
  if (myLen >= len)
    return false;

  Log::trace("Allocate shared buffer '%s' | %d bytes", mySharedMemName, len);

  _releaseSharedMem();

  const int additionalBytes = 1024;
  mySharedSegment = new managed_shared_memory(create_only, mySharedMemName, len + additionalBytes);
  managed_shared_memory::size_type free_memory = mySharedSegment->get_free_memory();
  mySharedMem = mySharedSegment->allocate(len);
  myLen = len;

  // Check invariant
  if(free_memory <= mySharedSegment->get_free_memory()) {
    Log::error("free_memory %d <= mySharedSegment->get_free_memory() %d", free_memory, mySharedSegment->get_free_memory());
    _releaseSharedMem();
    return false;
  }

  mySharedMemHandle = mySharedSegment->get_handle_from_address(mySharedMem);
  return true;
}

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
    myOwner.exec([&](RpcExecutor::Service s){
      s->RenderHandler_GetViewRect(result, myOwner.getBid());
    });
    if (result.w < 0) return;

    rect.x = result.x;
    rect.y = result.y;
    rect.width = result.w;
    rect.height = result.h;

    if (rect.width < 1 || rect.height < 1) {
        Log::error("small size %d %d", rect.width, rect.height);
        fillDummy(rect);
    }
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
    myOwner.exec([&](RpcExecutor::Service s){
      s->RenderHandler_GetScreenInfo(result, myOwner.getBid());
    });
    if (result.depth == -1) return false;

    screen_info.device_scale_factor = result.device_scale_factor;
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
    myOwner.exec([&](RpcExecutor::Service s){
      s->RenderHandler_GetScreenPoint(result, myOwner.getBid(), viewX, viewY);
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

inline void fillRect(unsigned char * dst, int stride, int y, int x, int dx, int dy, int r, int g, int b, int a) {
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

void RemoteRenderHandler::OnPaint(CefRefPtr<CefBrowser> browser,
                            PaintElementType type,
                            const RectList& dirtyRects,
                            const void* buffer,
                            int width,
                            int height) {
#ifdef LOG_PAINT
    Measurer measurer;
    LogNdc ndc("RemoteRenderHandler", string_format("OnPaint(w=%d,h=%d), rects=%d", width, height, dirtyRects.size()));
#endif
    const int rasterPixCount = width*height;
    const int extendedRectsCount = dirtyRects.size() < 10 ? 10 : dirtyRects.size();
    const bool reallocated = _ensureSharedCapacity(rasterPixCount*4 + 4*4*extendedRectsCount);
    if (mySharedMem == nullptr) return;

    // write rects and flipped raster
    int rectsCount = dirtyRects.size();
    const int stride = width*4;
    int32_t * sharedRects = (int32_t *)mySharedMem + rasterPixCount;
    if (dirtyRects.empty() || reallocated) {
#ifdef LOG_PAINT
        measurer.append(": full copy");
#endif
        rectsCount = 0;
        for (int y = 0; y < height; ++y)
          ::memcpy(((char*)mySharedMem) + (height - y - 1)*stride, ((char*)buffer) + y*stride, stride);
    } else {
#ifdef LOG_PAINT
        measurer.append(": ");
#endif

        // NOTE: single memcpy takes the same time as line-by-line copy (tested on macbook pro m1)
        // TODO: premultiply alpha in this loop
        for (const CefRect& r : dirtyRects) {
          for (int y = r.y, yEnd = r.y + r.height; y < yEnd; ++y)
            ::memcpy(((char*)mySharedMem) + (height - y - 1)*stride + r.x*4, ((char*)buffer) + y*stride + r.x*4, r.width*4);

          char str[128];
          ::sprintf(str, "[%d,%d,%d,%d], ", r.x, r.y, r.width, r.height);

          *(sharedRects++) = r.x;
          *(sharedRects++) = height - (r.y + r.height);
          *(sharedRects++) = r.width;
          *(sharedRects++) = r.height;

#ifdef LOG_PAINT
          measurer.append(str);
#endif
        }
    }

#ifdef DRAW_DEBUG
    fillRect((unsigned char *)mySharedMem, stride, 0, 0, 10, 10, 255, 0, 0, 255);
    fillRect((unsigned char *)mySharedMem, stride, 0, width - 10, 10, 10, 0, 255, 0, 255);
    fillRect((unsigned char *)mySharedMem, stride, height - 10, width - 10, 10, 10, 0, 0, 255, 255);
    fillRect((unsigned char *)mySharedMem, stride, height - 10, 0, 10, 10, 255, 0, 255, 255);
#endif //DRAW_DEBUG

    {
#ifdef LOG_PAINT
        Measurer measurer2("RPC");
#endif
        myOwner.exec([&](RpcExecutor::Service s){
          s->RenderHandler_OnPaint(myOwner.getBid(), type == PET_VIEW ? false : true, rectsCount,
                     mySharedMemName, mySharedMemHandle, reallocated,
                     width, height);
        });
    }
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
