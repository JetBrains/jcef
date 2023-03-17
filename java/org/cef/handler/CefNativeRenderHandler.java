package org.cef.handler;

import org.cef.browser.CefBrowser;

import java.awt.*;

public interface CefNativeRenderHandler extends CefRenderHandler {
    void onPaintWithSharedMem(CefBrowser browser, boolean popup, int dirtyRectsCount,
                         String sharedMemName, long boostHandle, boolean recreateHandle, int width, int height);
}
