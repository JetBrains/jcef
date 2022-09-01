package tests.junittests;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.misc.CefLog;

import javax.swing.*;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author tav
 */
public class JBCefBrowser {
    private final CefBrowser myCefBrowser;
    private final CefClient myCefClient;

    private volatile boolean myIsCefBrowserCreated;
    private volatile LoadDeferrer myLoadDeferrer;

    private static class LoadDeferrer {
        protected final String myHtml;
        protected final String myUrl;

        private LoadDeferrer(String html, String url) {
            myHtml = html;
            myUrl = url;
        }

        public static LoadDeferrer urlDeferrer(String url) {
            return new LoadDeferrer(null, url);
        }

        public static LoadDeferrer htmlDeferrer(String html, String url) {
            return new LoadDeferrer(html, url);
        }

        public void load(CefBrowser browser) {
            // JCEF demands async loading.
            SwingUtilities.invokeLater(
                    myHtml == null ?
                            () -> browser.loadURL(myUrl) :
                            () -> loadString(browser, myHtml, myUrl));
        }
    }

    public JBCefBrowser(CefLoadHandler loadHandler) {
        myCefClient = CefApp.getInstance().createClient();

        myCefClient.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                CefLog.Info("CefLifeSpanHandler.onAfterCreated, browser " + browser);
                myIsCefBrowserCreated = true;
                LoadDeferrer loader = myLoadDeferrer;
                if (loader != null) {
                    loader.load(browser);
                    myLoadDeferrer = null;
                }
            }
        });
        if (loadHandler != null)
            myCefClient.addLoadHandler(loadHandler);

        myCefBrowser = myCefClient.createBrowser("about:blank", false, false);
    }

    public Component getComponent() {
        return myCefBrowser.getUIComponent();
    }

    public void loadURL(String url) {
        if (myIsCefBrowserCreated) {
            myCefBrowser.loadURL(url);
        }
        else {
            myLoadDeferrer = LoadDeferrer.urlDeferrer(url);
        }
    }

    public void loadHTML(String html, String url) {
        if (myIsCefBrowserCreated) {
            loadString(myCefBrowser, html, url);
        }
        else {
            myLoadDeferrer = LoadDeferrer.htmlDeferrer(html, url);
        }
    }

    private static void loadString(CefBrowser cefBrowser, String html, String url) {
        CefLog.Error("jcef: loadString: " + html);
        throw new UnsupportedOperationException("not yet supported in tests");
    }

    public void loadHTML(String html) {
        loadHTML(html, "about:blank");
    }

    public CefClient getCefClient() {
        return myCefClient;
    }

    public CefBrowser getCefBrowser() {
        return myCefBrowser;
    }

    public void dispose() {
        myCefBrowser.close(true);
        myCefClient.dispose();
    }
}
