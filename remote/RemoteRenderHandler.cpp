#include "RemoteRenderHandler.h"

#include <iostream>
#include <chrono>

#include "log/Log.h"
#include "CefUtils.h"

using namespace std::chrono;
using namespace remote;

RemoteRenderHandler::RemoteRenderHandler(const std::shared_ptr<ClientHandlersClient>& client, int bid)
    : myClient(client), myBid(bid) {}

bool RemoteRenderHandler::GetRootScreenRect(CefRefPtr<CefBrowser> browser,
                                      CefRect& rect) {
    GetViewRect(browser, rect);
    return rect.width > 1;
}

void fillDummy(CefRect& rect) {
    rect.x = 0;
    rect.y = 0;
    rect.width = 500;
    rect.height = 700;
}

void RemoteRenderHandler::GetViewRect(CefRefPtr<CefBrowser> browser, CefRect& rect) {
    Measurer measurer("RemoteRenderHandler::GetViewRect");

    std::string result;
    myClient->getInfo(result, myBid, "viewRect", "");
    const int len = result.size();
    if (len < 4) {
        Log::error(": len < 4");
        fillDummy(rect);
        return;
    }

    const int32_t * p = (const int32_t *)result.c_str();
    rect.x = *(p++);
    rect.y = *(p++);
    rect.width = *(p++);
    rect.height = *(p++);
    if (rect.width < 1 || rect.height < 1) {
        Log::error("small size");
        fillDummy(rect);
        return;
    }

    measurer.append(" rc=" + toString(rect));
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
    Measurer measurer("RemoteRenderHandler::GetScreenInfo");

    std::string result;
    myClient->getInfo(result, myBid, "screenInfo", "");

    const int len = result.size();
    if (len <= 1) {
        Log::warn("len %d <= 1", len);
        fillDummy(screen_info);
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

    measurer.append(" rc=" + toString(screen_info.rect) + "; arc=" + toString(screen_info.available_rect));
    return true;
}

bool RemoteRenderHandler::GetScreenPoint(CefRefPtr<CefBrowser> browser,
                                   int viewX,
                                   int viewY,
                                   int& screenX,
                                   int& screenY) {
    Measurer measurer(string_format("RemoteRenderHandler::GetScreenPoint(%d,%d)", viewX, viewY));

    int32_t argsarr[2] = {viewX, viewY};
    std::string args((const char *)argsarr, sizeof(argsarr));
    std::string result;
    myClient->getInfo(result, myBid, "screenPoint", args);
    const int32_t * p = (const int32_t *)result.c_str();
    screenX = *p;
    screenY = *(p + 1);

    measurer.append(string_format(" pt=", screenX, screenY));
    return true;
}

void RemoteRenderHandler::OnPopupShow(CefRefPtr<CefBrowser> browser, bool show) {

}

void RemoteRenderHandler::OnPopupSize(CefRefPtr<CefBrowser> browser,
                                const CefRect& rect) {

}

void RemoteRenderHandler::OnPaint(CefRefPtr<CefBrowser> browser,
                            PaintElementType type,
                            const RectList& dirtyRects,
                            const void* buffer,
                            int width,
                            int height) {
    Measurer measurer(string_format("RemoteRenderHandler::OnPaint(%d,%d)", width, height));

    std::vector<int32_t> rects;
    for (const CefRect & r: dirtyRects) {
        rects.push_back(r.x);
        rects.push_back(r.y);
        rects.push_back(r.width);
        rects.push_back(r.height);
    }
    std::string srects((const char *)rects.data(), rects.size()*sizeof(int32_t));

    std::stringstream buf;
    const int size = width*height*4;
    buf.write(static_cast<const char *>(buffer), size);

    myClient->onPaint(myBid, type == PET_VIEW ? false : true, srects, buf.str(), width, height);
}

bool RemoteRenderHandler::StartDragging(CefRefPtr<CefBrowser> browser,
                                  CefRefPtr<CefDragData> drag_data,
                                  DragOperationsMask allowed_ops,
                                  int x,
                                  int y) {
    return true;
}

void RemoteRenderHandler::UpdateDragCursor(CefRefPtr<CefBrowser> browser,
                                     DragOperation operation) {

}

