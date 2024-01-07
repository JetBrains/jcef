package org.cef.handler;

import org.cef.browser.CefBrowser;

public interface CefNativeRenderHandler extends CefRenderHandler {
    void onPaintWithSharedMem(CefBrowser browser, boolean popup, int dirtyRectsCount,
                         String sharedMemName, long boostHandle, int width, int height);
    void disposeNativeResources();
}
