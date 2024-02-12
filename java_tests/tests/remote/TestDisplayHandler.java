package tests.remote;

import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandler;
import org.cef.misc.CefLog;

public class TestDisplayHandler implements CefDisplayHandler {
    @Override
    public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
        CefLog.Info("onAddressChange " + browser + ", " + url);
    }

    @Override
    public void onTitleChange(CefBrowser browser, String title) {
        CefLog.Info("onTitleChange " + browser + ", " + title);
    }

    @Override
    public void onFullscreenModeChange(CefBrowser browser, boolean fullscreen) {
        CefLog.Info("onTitleChange " + browser + ", " + fullscreen);
    }

    @Override
    public boolean onTooltip(CefBrowser browser, String text) {
        CefLog.Info("onTooltip " + browser + ", " + text);
        return false;
    }

    @Override
    public void onStatusMessage(CefBrowser browser, String value) {
        CefLog.Info("onStatusMessage " + browser + ", " + value);
    }

    @Override
    public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
        CefLog.Info("onConsoleMessage " + browser + ", " + message + ", " + source + ", " + line);
        return false;
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        CefLog.Info("onCursorChange " + browser + ", " + cursorType);
        return false;
    }
}
