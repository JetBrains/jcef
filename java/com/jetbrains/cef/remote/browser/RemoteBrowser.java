package com.jetbrains.cef.remote.browser;

import com.jetbrains.cef.remote.PlatformUtils;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.RpcContext;
import com.jetbrains.cef.remote.callback.RemoteIntCallback;
import com.jetbrains.cef.remote.callback.RemotePdfPrintCallback;
import com.jetbrains.cef.remote.callback.RemoteRunFileDialogCallback;
import com.jetbrains.cef.remote.callback.RemoteStringVisitor;
import com.jetbrains.cef.remote.network.RemoteRequest;
import com.jetbrains.cef.remote.network.RemoteRequestContext;
import com.jetbrains.cef.remote.network.RemoteRequestImpl;
import com.jetbrains.cef.remote.thrift_codegen.CompositionUnderline;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import com.jetbrains.cef.remote.thrift_codegen.Range;
import com.jetbrains.cef.remote.thrift_codegen.Style;
import org.cef.CefApp;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefNativeRenderHandler;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefWindowHandler;
import org.cef.input.CefCompositionUnderline;
import org.cef.input.CefTouchEvent;
import org.cef.misc.CefLog;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.misc.CefRange;
import org.cef.network.CefRequest;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class RemoteBrowser implements CefBrowser {
    private final RpcContext myRpc;
    private final RemoteClient myOwner;
    private final CefClient myCefClient; // will be the "owner" of RemoteClient, needed to override getClient()
    private final RemoteRequestContext myRequestContext;
    private final CefBrowserSettings mySettings; // TODO: use settings in startNativeCreation

    private volatile int myBid = -1;
    private String myUrl = null;
    private Component myComponent;
    private CefNativeRenderHandler myRender;
    private Supplier<CefRendering> myRenderingFactory; // necessary for dev-tools browser creation

    private final AtomicBoolean myIsNativeBrowserCreationRequested = new AtomicBoolean(false);
    private final AtomicBoolean myIsNativeBrowserCreationStarted = new AtomicBoolean(false);
    private volatile boolean myIsNativeBrowserCreated = false;
    private volatile boolean myIsClosing = false;
    private volatile boolean myIsClosed = false;
    private volatile int myNativeBrowserIdentifier = Integer.MIN_VALUE;

    private final List<Runnable> myDelayedActions = new ArrayList<>();
    private int myFrameRate = 30; // just for cache

    private volatile RemoteBrowser myDevTools = null;
    private volatile RemoteBrowser myParentBrowser = null;
    private volatile CefDevToolsClient myDevToolsClient = null;
    private Point myInspectPoint;

    RemoteBrowser(RpcContext rpcContext, RemoteClient owner, CefClient cefClient, String url, RemoteRequestContext requestContext, CefBrowserSettings settings) {
        myRpc = rpcContext;
        myOwner = owner;
        myCefClient = cefClient;
        myUrl = url;
        myRequestContext = requestContext != null ? requestContext : new RemoteRequestContext(myRpc.server);
        mySettings = settings;
    }

    public int getBid() { return myBid; }
    public int getCid() { return myOwner.getCid(); }
    public RemoteClient getOwner() { return myOwner; }

    public boolean isNativeBrowserCreationStarted() { return myIsNativeBrowserCreationStarted.get(); }
    public boolean isNativeBrowserCreated() { return myIsNativeBrowserCreated; }
    public int getNativeBrowserIdentifier() { return myNativeBrowserIdentifier; }

    protected void setNativeBrowserCreated(int nativeBrowserIdentifier) {
        // Called from lifespan-handler::onAfterCreated (of owner)
        synchronized (myDelayedActions) {
            myIsNativeBrowserCreated = true;
            myNativeBrowserIdentifier = nativeBrowserIdentifier;
            myDelayedActions.forEach(r -> r.run());
            myDelayedActions.clear();
        }
    }

    public void setComponent(Component component, CefNativeRenderHandler renderHandler) {
        myComponent = component;
        myRender = renderHandler;
    }

    public void setRenderingFactory(Supplier<CefRendering> renderingFactory) {
        myRenderingFactory = renderingFactory;
    }

    private void execWhenCreated(Runnable runnable, String name) {
        synchronized (myDelayedActions) {
            if (myIsNativeBrowserCreated) {
                runnable.run();
            } else {
                CefLog.Debug("%s: add delayed action %s", this, name);
                myDelayedActions.add(runnable);
            }
        }
    }

    @Override
    public void createImmediately() {
        if (!myIsNativeBrowserCreationRequested.getAndSet(true))
            myRpc.server.onConnected(this::requestBid, "requestBid", false);
    }

    private void requestBid() {
        synchronized (myIsNativeBrowserCreationStarted) {
            if (myIsClosing)
                return;

            myIsNativeBrowserCreationStarted.set(true);
            final int hmask = myOwner.getHandlersMask() | (myRender == null ? 0 :
                    RemoteClient.HandlerMasks.NativeRender.val());
            myRpc.exec((s) -> {
                RObject contextHandler = new RObject();
                if (myRequestContext.getRemoteHandler() != null)
                    contextHandler = myRequestContext.getRemoteHandler().thriftId();
                myBid = s.Browser_Create(myOwner.getCid(), hmask, contextHandler);
            });
            if (myBid >= 0) {
                myOwner.onNewBid(this);
                CefLog.Debug("Registered bid %d with handlers: %s", myBid, RemoteClient.HandlerMasks.toString(hmask));
                // At current point new bid is registered so java-handlers calls will be dispatched correctly.
                // We can't start creation earlier because for example onAfterCreated can be called before new bid is registered.
                if (myParentBrowser != null)
                    myRpc.exec((s) -> s.Browser_StartNativeDevToolsCreation(myBid, myParentBrowser.getBid(), myInspectPoint.x ,myInspectPoint.x));
                else
                    myRpc.exec((s) -> s.Browser_StartNativeCreation(myBid, myUrl));
            } else
                CefLog.Error("Can't obtain bid, createBrowser returns %d", myBid);
        }

        if (myBid >= 0)
            myRequestContext.setBid(myBid, myRpc);
    }

    @Override
    public Component getUIComponent() {
        return myComponent;
    }

    @Override
    public CefClient getClient() {
        return myCefClient;
    }

    @Override
    public CefRequestContext getRequestContext() { return myRequestContext; }

    @Override
    public CefRenderHandler getRenderHandler() { return myRender; }

    @Override
    public CefWindowHandler getWindowHandler() {
        // Remote mode uses OSR only.
        return null;
    }

    @Override
    public boolean canGoBack() {
        if (myIsClosing || myBid < 0)
            return false;

        return myRpc.execObj(s-> s.Browser_CanGoBack(myBid));
    }

    @Override
    public void goBack() {
        if (myIsClosing || myBid < 0)
            return;

        myRpc.exec(s-> s.Browser_GoBack(myBid));
    }

    @Override
    public boolean canGoForward() {
        if (myIsClosing || myBid < 0)
            return false;

        return myRpc.execObj(s-> s.Browser_CanGoForward(myBid));
    }

    @Override
    public void goForward() {
        if (myIsClosing || myBid < 0)
            return;

        myRpc.exec(s-> s.Browser_GoForward(myBid));
    }

    @Override
    public boolean isLoading() {
        if (myIsClosing || myBid < 0)
            return false;

        return myRpc.execObj(s-> s.Browser_IsLoading(myBid));
    }

    @Override
    public void reload() {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_Reload(myBid);
            });
        }, "reload");
    }

    @Override
    public void reloadIgnoreCache() {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_ReloadIgnoreCache(myBid);
            });
        }, "reloadIgnoreCache");
    }

    @Override
    public void stopLoad() {
        if (myIsClosing || myBid < 0)
            return;

        myRpc.exec(s-> s.Browser_StopLoad(myBid));
    }

    @Override
    public int getIdentifier() {
        return myNativeBrowserIdentifier;
    }

    @Override
    public CefFrame getMainFrame() {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getMainFrame will return null.");
            return null;
        }

        RObject rf = myRpc.execObj(s-> s.Browser_GetMainFrame(myBid));
        return rf == null || rf.isNull ? null : new RemoteFrame(myRpc, rf);
    }

    @Override
    public CefFrame getFocusedFrame() {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFocusedFrame will return null.");
            return null;
        }

        RObject rf = myRpc.execObj(s-> s.Browser_GetFocusedFrame(myBid));
        return rf == null || rf.isNull ? null : new RemoteFrame(myRpc, rf);
    }

    @Override
    public CefFrame getFrameByIdentifier(String identifier) {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFrameByIdentifier will return null.");
            return null;
        }

        RObject rf = myRpc.execObj(s-> s.Browser_GetFrameByIdentifier(myBid, identifier));
        return rf == null || rf.isNull ? null : new RemoteFrame(myRpc, rf);
    }

    @Override
    public CefFrame getFrameByName(String name) {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFrameByName will return null.");
            return null;
        }

        RObject rf = myRpc.execObj(s-> s.Browser_GetFrameByName(myBid, name));
        return rf == null || rf.isNull ? null : new RemoteFrame(myRpc, rf);
    }

    @Override
    public Vector<String> getFrameIdentifiers() {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFrameIdentifiers will return null.");
            return null;
        }

        List<String> ids = myRpc.execObj(s-> s.Browser_GetFrameIdentifiers(myBid));
        return ids == null || ids.isEmpty() ? null : new Vector<>(ids);
    }

    @Override
    public Vector<String> getFrameNames() {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFrameNames will return null.");
            return null;
        }

        List<String> ids = myRpc.execObj(s-> s.Browser_GetFrameNames(myBid));
        return ids == null || ids.isEmpty() ? null : new Vector<>(ids);
    }

    @Override
    public int getFrameCount() {
        if (myIsClosing)
            return 0;

        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFrameCount will return 0.");
            return 0;
        }

        return myRpc.execObj(s-> s.Browser_GetFrameCount(myBid));
    }

    @Override
    public boolean isPopup() {
        if (myIsClosing || myBid < 0)
            return false;

        return myRpc.execObj(s-> s.Browser_IsPopup(myBid));
    }

    @Override
    public boolean hasDocument() {
        if (myIsClosing || myBid < 0)
            return false;

        return myRpc.execObj(s-> s.Browser_HasDocument(myBid));
    }

    @Override
    public void viewSource() {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_ViewSource(myBid);
            });
        }, "viewSource");
    }

    @Override
    public void getSource(CefStringVisitor visitor) {
        if (myIsClosing || visitor == null)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                RemoteStringVisitor rvisitor = RemoteStringVisitor.create(visitor);
                s.Browser_GetSource(myBid, rvisitor.thriftId());
            });
        }, "getSource");
    }

    @Override
    public void getText(CefStringVisitor visitor) {
        if (myIsClosing || visitor == null)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                RemoteStringVisitor rvisitor = RemoteStringVisitor.create(visitor);
                s.Browser_GetText(myBid, rvisitor.thriftId());
            });
        }, "getText");
    }

    @Override
    public void loadRequest(CefRequest request) {
        if (myIsClosing)
            return;

        if (!(request instanceof RemoteRequest)) {
            CefLog.Error("Unsupported CefRequest: %s", request);
            return;
        }

        execWhenCreated(() -> {
            RemoteRequestImpl rr = ((RemoteRequest)request).getImpl();
            if (rr != null) {
                rr.flush(); // just for insurance
                myRpc.exec((s) -> s.Browser_LoadRequest(myBid, rr.thriftIdWithCache()));
            } else
                CefLog.Error("RemoteRequestImpl is null [bid=%d]", myBid);
        }, "loadRequest");
    }

    @Override
    public void loadURL(String url) {
        myUrl = url;
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_LoadURL(myBid, url);
            });
        }, "loadURL");
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_ExecuteJavaScript(myBid, code, url, line);
            });
        }, "executeJavaScript");
    }

    @Override
    public String getURL() {
        if (myBid < 0) {
            CefLog.Debug("Can't do getURL because bid wasn't created, return cached %s", myUrl);
            return myUrl;
        }
        if (myIsClosing)
            return myUrl;

        return myRpc.execObj((s)->{
            return s.Browser_GetURL(myBid);
        });
    }

    @Override
    public void close(boolean force) {
        synchronized (myIsNativeBrowserCreationStarted) {
            if (myIsClosing)
                return;
            myIsClosing = true;
            if (myRender != null)
                myRender.disposeNativeResources();
            if (myBid >= 0)
                myRpc.exec(s -> s.Browser_Close(myBid));
        }
        synchronized (myDelayedActions) {
            myDelayedActions.clear();
        }
    }

    protected final void closeDevTools() {
        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_CloseDevTools(myBid);
            });
        }, "closeDevTools");
    }

    @Override
    public void setCloseAllowed() {}

    @Override
    public boolean doClose() { return false; }

    @Override
    public void onBeforeClose() {
        // Called from lifespan handler (before native browser disposed).
        myIsClosed = true;
        myRequestContext.dispose();
        if (myParentBrowser != null) {
            myParentBrowser.closeDevTools();
            myParentBrowser.myDevTools = null;
            myParentBrowser = null;
        }
        if (myDevToolsClient != null)
            myDevToolsClient.close();
    }

    @Override
    public boolean isClosing() { return myIsClosing; }

    @Override
    public boolean isClosed() { return myIsClosed; }

    @Override
    public void setFocus(boolean enable) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_SetFocus(myBid, enable);
            });
        }, "setFocus");
    }

    @Override
    public void setWindowVisibility(boolean visible) {
        // NOTE: doesn't used in OSR mode
    }

    @Override
    public double getZoomLevel() {
        if (myBid < 0) {
            CefLog.Debug("Can't do getZoomLevel because bid wasn't created, return 0");
            return 0;
        }
        if (myIsClosing)
            return 0;

        return myRpc.execObj((s)-> s.Browser_GetZoomLevel(myBid));
    }

    @Override
    public void setZoomLevel(double zoomLevel) {
        if (myIsClosing)
            return;

        execWhenCreated(()->myRpc.exec((s)-> s.Browser_SetZoomLevel(myBid, zoomLevel)), "setZoomLevel");
    }

    @Override
    public void runFileDialog(CefDialogHandler.FileDialogMode mode, String title, String defaultFilePath, Vector<String> acceptFilters, CefRunFileDialogCallback callback) {
        if (myIsClosing)
            return;
        if (callback == null) {
            CefLog.Error("Can't run file dialog because callback is null.");
            return;
        }
        RemoteRunFileDialogCallback rcallback = RemoteRunFileDialogCallback.create(callback);
        final Vector<String> filters = acceptFilters == null ? new Vector<>() : acceptFilters;
        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_RunFileDialog(myBid, mode.name(), title, defaultFilePath, filters, rcallback.thriftId());
            });
        }, "runFileDialog");
    }

    @Override
    public void startDownload(String url) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_StartDownload(myBid, url);
            });
        }, "startDownload");
    }

    @Override
    public void print() {
        if (myIsClosing)
            return;
        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_Print(myBid);
            });
        }, "print");
    }

    @Override
    public void printToPDF(String path, CefPdfPrintSettings settings, CefPdfPrintCallback callback) {
        if (myIsClosing)
            return;
        if (callback == null) {
            CefLog.Error("Can't print to pdf because callback is null.");
            return;
        }
        RemotePdfPrintCallback rcallback = RemotePdfPrintCallback.create(callback);
        Map<String, String> printSettings = new HashMap<>();
        if (settings != null) {
            printSettings.put("landscape", String.valueOf(settings.landscape));
            printSettings.put("print_background", String.valueOf(settings.print_background));
            printSettings.put("scale", String.valueOf(settings.scale));
            printSettings.put("paper_width", String.valueOf(settings.paper_width));
            printSettings.put("paper_height", String.valueOf(settings.paper_height));
            printSettings.put("prefer_css_page_size", String.valueOf(settings.prefer_css_page_size));
            if (settings.margin_type != null)
                printSettings.put("margin_type", String.valueOf(settings.margin_type));
            printSettings.put("margin_top", String.valueOf(settings.margin_top));
            printSettings.put("margin_bottom", String.valueOf(settings.margin_bottom));
            printSettings.put("margin_right", String.valueOf(settings.margin_right));
            printSettings.put("margin_left", String.valueOf(settings.margin_left));
            if (settings.page_ranges != null && !settings.page_ranges.isEmpty())
                printSettings.put("page_ranges", settings.page_ranges);
            printSettings.put("display_header_footer", String.valueOf(settings.display_header_footer));
            if (settings.header_template != null && !settings.header_template.isEmpty())
                printSettings.put("header_template", settings.header_template);
            if (settings.footer_template != null && !settings.footer_template.isEmpty())
                printSettings.put("footer_template", settings.footer_template);
        }
        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_PrintToPDF(myBid, path, printSettings, rcallback.thriftId());
            });
        }, "printToPDF");
    }

    @Override
    public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_Find(myBid, searchText, forward, matchCase, findNext);
            });
        }, "find");
    }

    @Override
    public void stopFinding(boolean clearSelection) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_StopFinding(myBid, clearSelection);
            });
        }, "stopFinding");
    }

    @Override
    public CefBrowser getDevTools() { return getDevTools(null); }

    @Override
    public CefBrowser getDevTools(Point inspectAt) {
        if (myIsClosing)
            return null;

        if (myDevTools == null) {
            if (myRenderingFactory == null) {
                CefLog.Error("Can't create dev-tools browser because rendering factory is null. Please use proper constructor (or invoke setRenderingFactory()).");
                return null;
            }
            myDevTools = myOwner.createBrowser(myUrl, myRequestContext, myCefClient, myRenderingFactory.get(), mySettings);
            myDevTools.myParentBrowser = this;
            myDevTools.myInspectPoint = inspectAt == null ? new Point(0, 0) : inspectAt;
        }
        return myDevTools;
    }

    @Override
    public CefDevToolsClient getDevToolsClient() {
        if (myIsClosing)
            return null;

        if (myDevToolsClient == null || myDevToolsClient.isClosed())
            myDevToolsClient = new CefDevToolsClient(this);
        return myDevToolsClient;
    }

    @Override
    public void replaceMisspelling(String word) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_ReplaceMisspelling(myBid, word);
            });
        }, "replaceMisspelling");
    }

    @Override
    public void wasResized(int width/*unused*/, int height/*unused*/) {
        // NOTE: width, height are unused.
        // This method will schedule request of new size via CefRenderHandler.GetViewRect.
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.invokeLater(s -> s.Browser_WasResized(myBid));
        }, "wasResized");
    }

    @Override
    public void notifyScreenInfoChanged() {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.invokeLater(s -> s.Browser_NotifyScreenInfoChanged(myBid));
        }, "notifyScreenInfoChanged");
    }

    @Override
    public void sendKeyEvent(KeyEvent e) {
        if (myBid < 0) {
            CefLog.Debug("Skip sendKeyEvent because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        if (myIsClosing)
            return;

        var cefKeyEvent = PlatformUtils.getCefKeyEventAttributes(e);

        myRpc.invokeLater(s -> s.Browser_SendCefKeyEvent(myBid, cefKeyEvent));
    }

    @Override
    public void sendMouseEvent(MouseEvent e) {
        if (myBid < 0) {
            CefLog.Debug("Skip sendMouseEvent because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        if (myIsClosing)
            return;

        myRpc.invokeLater(s -> s.Browser_SendMouseEvent(myBid, e.getID(), e.getX(), e.getY(), e.getModifiersEx(), e.getClickCount(), e.getButton()));
    }

    @Override
    public void sendMouseWheelEvent(MouseWheelEvent e) {
        if (myBid < 0) {
            CefLog.Debug("Skip sendMouseWheelEvent because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        if (myIsClosing)
            return;

        myRpc.invokeLater(s -> s.Browser_SendMouseWheelEvent(myBid, e.getScrollType(), e.getX(), e.getY(), e.getModifiersEx(), e.getWheelRotation(), e.getUnitsToScroll()));
    }

    @Override
    public void sendTouchEvent(CefTouchEvent e) {
        CefLog.Error("UNIMPLEMENTED: sendTouchEvent");
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        return null;
    }

    @Override
    public void ImeSetComposition(String text, List<CefCompositionUnderline> cefUnderlines,
                                  CefRange cefReplacementRange, CefRange cefSelectionRange) {
        List<CompositionUnderline> underlineList = new ArrayList<>();
        if (cefUnderlines != null) {
            for (var cefUnderline : cefUnderlines) {
                Range range = new Range(cefUnderline.getRange().from, cefUnderline.getRange().to);

                com.jetbrains.cef.remote.thrift_codegen.Color color =
                        new com.jetbrains.cef.remote.thrift_codegen.Color(
                                cefUnderline.getColor().getRed(),
                                cefUnderline.getColor().getGreen(),
                                cefUnderline.getColor().getBlue(),
                                cefUnderline.getColor().getAlpha());

                com.jetbrains.cef.remote.thrift_codegen.Color backgroundColor =
                        new com.jetbrains.cef.remote.thrift_codegen.Color(
                                cefUnderline.getBackgroundColor().getRed(),
                                cefUnderline.getBackgroundColor().getGreen(),
                                cefUnderline.getBackgroundColor().getBlue(),
                                cefUnderline.getBackgroundColor().getAlpha());
                Style style = Style.NONE;

                switch (cefUnderline.getStyle()) {
                    case SOLID -> style = Style.SOLID;
                    case DOT -> style = Style.DOT;
                    case DASH -> style = Style.DASH;
                }

                underlineList.add(new CompositionUnderline(
                        range, color, backgroundColor, cefUnderline.getThick(), style));
            }
        }

        Range replacementRange = new Range(cefReplacementRange.from, cefReplacementRange.to);
        Range selectionRange = new Range(cefSelectionRange.from, cefSelectionRange.to);

        myRpc.invokeLater(s -> s.Browser_ImeSetComposition(myBid, text, underlineList, replacementRange, selectionRange));
    }

    @Override
    public void ImeCommitText(String text, CefRange cefReplacementRange, int relativeCursorPos) {
        Range replacementRange = new Range(cefReplacementRange.from, cefReplacementRange.to);
        myRpc.invokeLater(s -> s.Browser_ImeCommitText(myBid, text, replacementRange, relativeCursorPos));
    }

    @Override
    public void ImeFinishComposingText(boolean b) {
        myRpc.invokeLater(s -> s.Browser_ImeFinishComposingText(myBid, b));
    }

    @Override
    public void ImeCancelComposing() {
        myRpc.invokeLater(s -> s.Browser_ImeCancelComposing(myBid));
    }

    @Override
    public void setWindowlessFrameRate(int frameRate) {
        myFrameRate = frameRate;

        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myRpc.exec((s)->{
                s.Browser_SetFrameRate(myBid, frameRate);
            });
        }, "setWindowlessFrameRate");
    }

    @Override
    public CompletableFuture<Integer> getWindowlessFrameRate() {
        CefLog.Warn("%s: getWindowlessFrameRate returns cached value %d. TODO: implement real getWindowlessFrameRate.", this, myFrameRate);
        CompletableFuture<Integer> result = new CompletableFuture<Integer>();
        result.complete(myFrameRate);
        return result;
    }

    @Override
    public String toString() {
        return "RemoteBrowser_" + myBid;
    }

    public CefRegistration addDevToolsMessageObserver(CefDevToolsMessageObserver observer) {
        if (myIsClosing || observer == null)
            return null;

        if (!myIsNativeBrowserCreated) {
            CefLog.Error("Can't add DevToolsMessageObserver because native browser wasn't created");
            return null;
        }

        RemoteDevToolsMessageObserver robserver = RemoteDevToolsMessageObserver.create(observer);
        RObject registration = myRpc.execObj(s -> s.Browser_AddDevToolsMessageObserver(myBid, robserver.thriftId()));
        RemoteRegistrationImpl impl = new RemoteRegistrationImpl(myRpc, registration);
        return new RemoteRegistration(impl);
    }

    public CompletableFuture<Integer> executeDevToolsMethod(String method, String parametersAsJson) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        if (myIsClosing || method == null) {
            future.completeExceptionally(new CefDevToolsClient.DevToolsException(myIsClosing ? "Client is closing." : "Method is null."));
        } else if (!myIsNativeBrowserCreated) {
            CefLog.Error("Can't execute DevToolsMethod because native browser wasn't created");
            future.completeExceptionally(new CefDevToolsClient.DevToolsException("Native browser wasn't created"));
        } else {
            RemoteIntCallback ricb = RemoteIntCallback.create(generatedMessageId -> {
                if (generatedMessageId <= 0) {
                    future.completeExceptionally(new CefDevToolsClient.DevToolsException(
                            String.format("Failed to execute DevTools method %s, generatedMessageId=%d", method, generatedMessageId)));
                } else {
                    future.complete(generatedMessageId);
                }
            });
            execWhenCreated(() -> {
                myRpc.exec(s -> s.Browser_ExecuteDevToolsMethod(myBid, method, parametersAsJson, ricb.thriftId()));
            }, String.format("executeDevToolsMethod: %s(%s)", method, parametersAsJson));
        }

        return future;
    }

    @Override
    public boolean isWindowless() {
        return true;
    }
}
