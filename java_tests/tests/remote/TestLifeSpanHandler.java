package tests.remote;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandler;
import org.cef.misc.CefLog;

public class TestLifeSpanHandler implements CefLifeSpanHandler {
    @Override
    public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
        CefLog.Info("onBeforePopup " + browser);
        return false;
    }
    @Override
    public void onAfterCreated(CefBrowser browser) {
        CefLog.Info("onAfterCreated " + browser);
    }

    @Override
    public boolean doClose(CefBrowser browser) {
        CefLog.Info("doClose " + browser);
        return false;
    }
    @Override
    public void onBeforeClose(CefBrowser browser) {
        CefLog.Info("onBeforeClose " + browser);
    }

    @Override
    public void onAfterParentChanged(CefBrowser browser) {
        CefLog.Error("onAfterParentChanged " + browser + ", mustn't be called (because of used OSR).");
    }
}
