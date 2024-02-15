package tests;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefRendering;
import org.cef.handler.CefRenderHandler;

public class OsrSupport {
    public static boolean isEnabled() {
        return CefApp.isRemoteEnabled() || Boolean.getBoolean("jcef.tests.osr");
    }

    public static CefBrowser createBrowser(CefClient client, String startURL) {
        JBCefOsrComponent osrComponent = new JBCefOsrComponent();
        JBCefOsrHandler osrHandler = new JBCefOsrHandler(osrComponent, null);
        CefBrowser browser = client.createBrowser(startURL, new CefRendering.CefRenderingWithHandler(osrHandler, osrComponent), false);
        osrComponent.setBrowser(browser);
        return browser;
    }
}
