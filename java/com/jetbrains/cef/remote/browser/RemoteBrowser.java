package com.jetbrains.cef.remote.browser;

import com.jetbrains.cef.remote.callback.RemoteStringVisitor;
import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.PlatformUtils;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.callback.RemoteIntCallback;
import com.jetbrains.cef.remote.network.RemoteCookieAccessFilter;
import com.jetbrains.cef.remote.network.RemoteRequest;
import com.jetbrains.cef.remote.network.RemoteRequestContext;
import com.jetbrains.cef.remote.network.RemoteRequestImpl;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class RemoteBrowser implements CefBrowser {
    private final RpcExecutor myService;
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

    RemoteBrowser(RpcExecutor service, RemoteClient owner, CefClient cefClient, String url, RemoteRequestContext requestContext, CefBrowserSettings settings) {
        myService = service;
        myOwner = owner;
        myCefClient = cefClient;
        myUrl = url;
        myRequestContext = requestContext != null ? requestContext : new RemoteRequestContext();
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
            CefServer.instance().onConnected(this::requestBid, "requestBid", false);
    }

    private void requestBid() {
        synchronized (myIsNativeBrowserCreationStarted) {
            if (myIsClosing)
                return;

            myIsNativeBrowserCreationStarted.set(true);
            final int hmask = myOwner.getHandlersMask() | (myRender == null ? 0 :
                    RemoteClient.HandlerMasks.NativeRender.val());
            myService.exec((s) -> {
                RObject contextHandler = new RObject(-1);
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
                    myService.exec((s) -> s.Browser_StartNativeDevToolsCreation(myBid, myParentBrowser.getBid(), myInspectPoint.x ,myInspectPoint.x));
                else
                    myService.exec((s) -> s.Browser_StartNativeCreation(myBid, myUrl));
            } else
                CefLog.Error("Can't obtain bid, createBrowser returns %d", myBid);
        }

        if (myBid >= 0)
            myRequestContext.setBid(myBid);
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

        return myService.execObj(s-> s.Browser_CanGoBack(myBid));
    }

    @Override
    public void goBack() {
        if (myIsClosing || myBid < 0)
            return;

        myService.exec(s-> s.Browser_GoBack(myBid));
    }

    @Override
    public boolean canGoForward() {
        if (myIsClosing || myBid < 0)
            return false;

        return myService.execObj(s-> s.Browser_CanGoForward(myBid));
    }

    @Override
    public void goForward() {
        if (myIsClosing || myBid < 0)
            return;

        myService.exec(s-> s.Browser_GoForward(myBid));
    }

    @Override
    public boolean isLoading() {
        if (myIsClosing || myBid < 0)
            return false;

        return myService.execObj(s-> s.Browser_IsLoading(myBid));
    }

    @Override
    public void reload() {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
                s.Browser_Reload(myBid);
            });
        }, "reload");
    }

    @Override
    public void reloadIgnoreCache() {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
                s.Browser_ReloadIgnoreCache(myBid);
            });
        }, "reloadIgnoreCache");
    }

    @Override
    public void stopLoad() {
        if (myIsClosing || myBid < 0)
            return;

        myService.exec(s-> s.Browser_StopLoad(myBid));
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

        RObject rf = myService.execObj(s-> s.Browser_GetMainFrame(myBid));
        return rf == null || rf.objId < 0 ? null : new RemoteFrame(myService, rf);
    }

    @Override
    public CefFrame getFocusedFrame() {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFocusedFrame will return null.");
            return null;
        }

        RObject rf = myService.execObj(s-> s.Browser_GetFocusedFrame(myBid));
        return rf == null || rf.objId < 0 ? null : new RemoteFrame(myService, rf);
    }

    @Override
    public CefFrame getFrameByIdentifier(String identifier) {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFrameByIdentifier will return null.");
            return null;
        }

        RObject rf = myService.execObj(s-> s.Browser_GetFrameByIdentifier(myBid, identifier));
        return rf == null || rf.objId < 0 ? null : new RemoteFrame(myService, rf);
    }

    @Override
    public CefFrame getFrameByName(String name) {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFrameByName will return null.");
            return null;
        }

        RObject rf = myService.execObj(s-> s.Browser_GetFrameByName(myBid, name));
        return rf == null || rf.objId < 0 ? null : new RemoteFrame(myService, rf);
    }

    @Override
    public Vector<String> getFrameIdentifiers() {
        if (myIsClosing)
            return null;
        if (myBid < 0) {
            CefLog.Debug("bid wasn't received yet and getFrameIdentifiers will return null.");
            return null;
        }

        List<String> ids = myService.execObj(s-> s.Browser_GetFrameIdentifiers(myBid));
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

        List<String> ids = myService.execObj(s-> s.Browser_GetFrameNames(myBid));
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

        return myService.execObj(s-> s.Browser_GetFrameCount(myBid));
    }

    @Override
    public boolean isPopup() {
        if (myIsClosing || myBid < 0)
            return false;

        return myService.execObj(s-> s.Browser_IsPopup(myBid));
    }

    @Override
    public boolean hasDocument() {
        if (myIsClosing || myBid < 0)
            return false;

        return myService.execObj(s-> s.Browser_HasDocument(myBid));
    }

    @Override
    public void viewSource() {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
                s.Browser_ViewSource(myBid);
            });
        }, "viewSource");
    }

    @Override
    public void getSource(CefStringVisitor visitor) {
        if (myIsClosing || visitor == null)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
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
            myService.exec((s)->{
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
                myService.exec((s) -> s.Browser_LoadRequest(myBid, rr.thriftIdWithCache()));
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
            myService.exec((s)->{
                s.Browser_LoadURL(myBid, url);
            });
        }, "loadURL");
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
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

        return myService.execObj((s)->{
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
                myService.exec(s -> s.Browser_Close(myBid));
        }
        synchronized (myDelayedActions) {
            myDelayedActions.clear();
        }
    }

    protected final void closeDevTools() {
        execWhenCreated(()->{
            myService.exec((s)->{
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
            myService.exec((s)->{
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

        return myService.execObj((s)-> s.Browser_GetZoomLevel(myBid));
    }

    @Override
    public void setZoomLevel(double zoomLevel) {
        if (myIsClosing)
            return;

        execWhenCreated(()->myService.exec((s)-> s.Browser_SetZoomLevel(myBid, zoomLevel)), "setZoomLevel");
    }

    @Override
    public void runFileDialog(CefDialogHandler.FileDialogMode mode, String title, String defaultFilePath, Vector<String> acceptFilters, CefRunFileDialogCallback callback) {
        CefLog.Error("TODO: implement runFileDialog.");
    }

    @Override
    public void startDownload(String url) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
                s.Browser_StartDownload(myBid, url);
            });
        }, "startDownload");
    }

    @Override
    public void print() {
        CefLog.Error("TODO: implement print.");
    }

    @Override
    public void printToPDF(String path, CefPdfPrintSettings settings, CefPdfPrintCallback callback) {
        CefLog.Error("TODO: implement printToPDF.");
    }

    @Override
    public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
                s.Browser_Find(myBid, searchText, forward, matchCase, findNext);
            });
        }, "find");
    }

    @Override
    public void stopFinding(boolean clearSelection) {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
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
            myService.exec((s)->{
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
            myService.exec((s)->{
                s.Browser_WasResized(myBid);
            });
        }, "wasResized");
    }

    @Override
    public void notifyScreenInfoChanged() {
        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
                s.Browser_NotifyScreenInfoChanged(myBid);
            });
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

        myService.exec(s -> s.Browser_SendCefKeyEvent(myBid, cefKeyEvent));
    }

    @Override
    public void sendMouseEvent(MouseEvent e) {
        if (myBid < 0) {
            CefLog.Debug("Skip sendMouseEvent because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_SendMouseEvent(myBid, e.getID(), e.getX(), e.getY(), e.getModifiersEx(), e.getClickCount(), e.getButton());
        });
    }

    @Override
    public void sendMouseWheelEvent(MouseWheelEvent e) {
        if (myBid < 0) {
            CefLog.Debug("Skip sendMouseWheelEvent because remote browser wasn't created, bid=%d", myBid);
            return;
        }
        if (myIsClosing)
            return;

        myService.exec((s)->{
            s.Browser_SendMouseWheelEvent(myBid, e.getScrollType(), e.getX(), e.getY(), e.getModifiersEx(), e.getWheelRotation(), e.getUnitsToScroll());
        });
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
    public void ImeSetComposition(String s, List<CefCompositionUnderline> list, CefRange cefRange, CefRange cefRange1) {
        CefLog.Error("ImeSetComposition is not implemented");
    }

    @Override
    public void ImeCommitText(String s, CefRange cefRange, int i) {
        CefLog.Error("ImeCommitText is not implemented");
    }

    @Override
    public void ImeFinishComposingText(boolean b) {
        CefLog.Error("ImeFinishComposingText is not implemented");
    }

    @Override
    public void ImeCancelComposing() {
        CefLog.Error("ImeCancelComposing is not implemented");
    }

    @Override
    public void setWindowlessFrameRate(int frameRate) {
        myFrameRate = frameRate;

        if (myIsClosing)
            return;

        execWhenCreated(()->{
            myService.exec((s)->{
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
        RObject registration = myService.execObj(s -> s.Browser_AddDevToolsMessageObserver(myBid, robserver.thriftId()));
        RemoteRegistrationImpl impl = new RemoteRegistrationImpl(myService, registration);
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
                myService.exec(s -> s.Browser_ExecuteDevToolsMethod(myBid, method, parametersAsJson, ricb.thriftId()));
            }, String.format("executeDevToolsMethod: %s(%s)", method, parametersAsJson));
        }

        return future;
    }

    @Override
    public boolean isWindowless() {
        return true;
    }
}
