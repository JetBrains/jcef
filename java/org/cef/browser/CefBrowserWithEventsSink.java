package org.cef.browser;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Interface to send events to the CEF directly.
 * You do not need it unless you are using {@link CefBrowserOsrWithHandler}
 */
public interface CefBrowserWithEventsSink extends CefBrowser {
    void wasResized(int width, int height);

    void sendKeyEvent(KeyEvent e);

    void sendMouseEvent(MouseEvent e);

    void sendMouseWheelEvent(MouseWheelEvent e);
}
