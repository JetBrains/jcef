#include "RemoteRenderHandler.h"
#include "RemoteClientHandler.h"

#include <iostream>

#include "../CefUtils.h"
#include "../log/Log.h"

using namespace std::chrono;
using namespace thrift_codegen;
using namespace boost::interprocess;

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
    LogNdc ndc("RemoteRenderHandler::GetViewRect");
    fillDummy(rect);
    std::string result;
    myOwner.exec([&](RpcExecutor::Service s){
      s->getInfo(result, myOwner.getBid(), "viewRect", "");
    });
    if (result.empty()) return;

    const int len = result.size();
    if (len < 4) {
        Log::error("len %d < 4", len);
        return;
    }

    const int32_t * p = (const int32_t *)result.c_str();
    rect.x = *(p++);
    rect.y = *(p++);
    rect.width = *(p++);
    rect.height = *(p++);

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
    LogNdc ndc("RemoteRenderHandler::GetScreenInfo");
    fillDummy(screen_info);
    std::string result;
    myOwner.exec([&](RpcExecutor::Service s){
      s->getInfo(result, myOwner.getBid(), "screenInfo", "");
    });
    if (result.empty()) return false;

    const int len = result.size();
    if (len <= 1) {
        Log::warn("len %d <= 1", len);
        return false;
    }

    const int32_t * p = (const int32_t *)result.data();
    screen_info.device_scale_factor = *(p++);
    screen_info.depth = *(p++);
    screen_info.depth_per_component = *(p++);
    screen_info.is_monochrome = *(p++);

    screen_info.rect.x = *(p++);
    screen_info.rect.y = *(p++);
    screen_info.rect.width = *(p++);
    screen_info.rect.height = *(p++);
    screen_info.available_rect.x = *(p++);
    screen_info.available_rect.y = *(p++);
    screen_info.available_rect.width = *(p++);
    screen_info.available_rect.height = *(p++);
    return true;
}

bool RemoteRenderHandler::GetScreenPoint(CefRefPtr<CefBrowser> browser,
                                   int viewX,
                                   int viewY,
                                   int& screenX,
                                   int& screenY) {
    LogNdc ndc(string_format("RemoteRenderHandler::GetScreenPoint(%d,%d)", viewX, viewY));
    int32_t argsarr[2] = {viewX, viewY};
    std::string args((const char *)argsarr, sizeof(argsarr));
    std::string result;
    myOwner.exec([&](RpcExecutor::Service s){
      s->getInfo(result, myOwner.getBid(), "screenPoint", args);
    });
    if (result.empty()) return false;

    const int32_t * p = (const int32_t *)result.c_str();
    screenX = *p;
    screenY = *(p + 1);
    return true;
}

void RemoteRenderHandler::OnPopupShow(CefRefPtr<CefBrowser> browser, bool show) {
    Log::error("RemoteRenderHandler::OnPopupShow: unimplemented");
}

void RemoteRenderHandler::OnPopupSize(CefRefPtr<CefBrowser> browser,
                                const CefRect& rect) {
    Log::error("RemoteRenderHandler::OnPopupSize: unimplemented");
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
    Measurer measurer;
    LogNdc ndc(string_format("RemoteRenderHandler::OnPaint(%d,%d), rc=%d", width, height, dirtyRects.size()));
    const int rasterPixCount = width*height;
    const int extendedRectsCount = dirtyRects.size() < 10 ? 10 : dirtyRects.size();
    const bool reallocated = _ensureSharedCapacity(rasterPixCount*4 + 4*4*extendedRectsCount);
    if (mySharedMem == nullptr) return;

    // write rects and flipped raster
    int rectsCount = dirtyRects.size();
    const int stride = width*4;
    int32_t * sharedRects = (int32_t *)mySharedMem + rasterPixCount;
    if (dirtyRects.empty() || reallocated) {
        measurer.append(": full copy");
        rectsCount = 0;
        for (int y = 0; y < height; ++y)
          ::memcpy(((char*)mySharedMem) + (height - y - 1)*stride, ((char*)buffer) + y*stride, stride);
    } else {
        measurer.append(": ");

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

          measurer.append(str);
        }
    }

#ifdef DRAW_DEBUG
    fillRect((unsigned char *)mySharedMem, stride, 0, 0, 10, 10, 255, 0, 0, 255);
    fillRect((unsigned char *)mySharedMem, stride, 0, width - 10, 10, 10, 0, 255, 0, 255);
    fillRect((unsigned char *)mySharedMem, stride, height - 10, width - 10, 10, 10, 0, 0, 255, 255);
    fillRect((unsigned char *)mySharedMem, stride, height - 10, 0, 10, 10, 255, 0, 255, 255);
#endif //DRAW_DEBUG

    {
        Measurer measurer2(string_format("remote-client"));
        myOwner.exec([&](RpcExecutor::Service s){
          s->onPaint(myOwner.getBid(), type == PET_VIEW ? false : true, rectsCount,
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
    Log::error("RemoteRenderHandler::StartDragging: unimplemented");
    return true;
}

void RemoteRenderHandler::UpdateDragCursor(CefRefPtr<CefBrowser> browser,
                                     DragOperation operation) {
    Log::error("RemoteRenderHandler::UpdateDragCursor: unimplemented");
}
