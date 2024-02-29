// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef;

import com.jetbrains.cef.remote.CefServer;
import com.jetbrains.cef.remote.callback.RemoteSchemeHandlerFactory;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefAppHandler;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefAppStateHandler;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Exposes static methods for managing the global CEF context.
 */
public class CefApp extends CefAppHandlerAdapter {
    public class CefVersion {
        public final int JCEF_COMMIT_NUMBER;

        public final String JCEF_COMMIT_HASH;

        public final int CEF_VERSION_MAJOR;
        public final int CEF_VERSION_MINOR;
        public final int CEF_VERSION_PATCH;
        public final int CEF_COMMIT_NUMBER;

        public final int CHROME_VERSION_MAJOR;
        public final int CHROME_VERSION_MINOR;
        public final int CHROME_VERSION_BUILD;
        public final int CHROME_VERSION_PATCH;

        protected CefVersion(int jcefCommitNo, String jcefCommitHash, int cefMajor, int cefMinor, int cefPatch,
                           int cefCommitNo, int chrMajor, int chrMin, int chrBuild, int chrPatch) {
            JCEF_COMMIT_NUMBER = jcefCommitNo;
            JCEF_COMMIT_HASH = jcefCommitHash;

            CEF_VERSION_MAJOR = cefMajor;
            CEF_VERSION_MINOR = cefMinor;
            CEF_VERSION_PATCH = cefPatch;
            CEF_COMMIT_NUMBER = cefCommitNo;

            CHROME_VERSION_MAJOR = chrMajor;
            CHROME_VERSION_MINOR = chrMin;
            CHROME_VERSION_BUILD = chrBuild;
            CHROME_VERSION_PATCH = chrPatch;
        }

        public String getJcefVersion() {
            return CEF_VERSION_MAJOR + "." + CEF_VERSION_MINOR + "." + CEF_VERSION_PATCH + "."
                    + JCEF_COMMIT_NUMBER + "." + JCEF_COMMIT_HASH;
        }

        public String getCefVersion() {
            return CEF_VERSION_MAJOR + "." + CEF_VERSION_MINOR + "." + CEF_VERSION_PATCH;
        }

        public String getChromeVersion() {
            return CHROME_VERSION_MAJOR + "." + CHROME_VERSION_MINOR + "." + CHROME_VERSION_BUILD
                    + "." + CHROME_VERSION_PATCH;
        }

        @Override
        public String toString() {
            return "JCEF Version = " + getJcefVersion() + "\n"
                    + "CEF Version = " + getCefVersion() + "\n"
                    + "Chromium Version = " + getChromeVersion();
        }
    }

    /**
     * The CefAppState gives you a hint if the CefApp is already usable or not
     * usable any more. See values for details.
     */
    public enum CefAppState {
        /**
         * No CefApp instance was created yet. Call getInstance() to create a new
         * one.
         */
        NONE,

        /**
         * CefApp is new created but not initialized yet. No CefClient and no
         * CefBrowser was created until now.
         */
        NEW,

        /**
         * CefApp is in its initializing process. Please wait until initializing is
         * finished.
         */
        INITIALIZING,

        /**
         * CefApp is up and running. At least one CefClient was created and the
         * message loop is running. You can use all classes and methods of JCEF now.
         */
        INITIALIZED,

        /**
         * CefApp is in its shutdown process. All CefClients and CefBrowser
         * instances will be disposed. No new CefClient or CefBrowser is allowed to
         * be created. The message loop will be performed until all CefClients and
         * all CefBrowsers are disposed completely.
         */
        SHUTTING_DOWN,

        /**
         * CefApp is terminated and can't be used any more. You can shutdown the
         * application safely now.
         */
        TERMINATED
    }

    /**
     * According to the singleton pattern, this attribute keeps
     * one single object of this class.
     */
    private static CefApp self = null;
    private static CefAppHandler appHandler_ = null;
    private static CefAppState state_ = CefAppState.NONE;
    private static final CompletableFuture<Void> ourStartupFeature = new CompletableFuture<>();
    private Timer workTimer_ = null;
    private final HashSet<CefClient> clients_ = new HashSet<CefClient>();
    private CefSettings settings_ = null;

    //
    // Background initialization support
    //
    private volatile boolean isInitialized_ = false;
    private final LinkedList<CefAppStateHandler> initializationListeners_ = new LinkedList<>();

    // Constants for testing JBR-5530
    private static final boolean PREINIT_ON_ANY_THREAD = Utils.getBoolean("jcef_app_preinit_any");
    private static final int PREINIT_TEST_DELAY_MS = Utils.getInteger("jcef_app_preinit_test_delay_ms", 0);
    private static final int INIT_TEST_DELAY_MS = Utils.getInteger("jcef_app_init_test_delay_ms", 0);

    // Support for JBR-4430
    private static final boolean IS_REMOTE_ENABLED = Boolean.getBoolean("jcef.remote.enabled");

    /**
     * To get an instance of this class, use the method
     * getInstance() instead of this CTOR.
     * <p>
     * The CTOR is called by getInstance() as needed and
     * loads all required JCEF libraries.
     */
    private CefApp(String[] args, CefSettings settings) {
        super(args);

        if (settings != null) settings_ = settings.clone();
        CefLog.init(settings);
        setState(CefAppState.NEW);

        ourStartupFeature.thenRunAsync(() -> {
            // Perform native pre-initialization.
            // This code will save global pointer to JVM instance.
            // Execute on the AWT event dispatching thread to store JNI context from EDT
            // NOTE: in practice it seems that this method can be called from any thread (at tests
            // execute successfully)
            // TODO: ensure and make all initialization steps in single bg thread.
            if (IS_REMOTE_ENABLED) {
                if (CefServer.connect(appHandler_, settings_)) {
                    CefLog.Debug("CefApp: native CefServer is initialized.");
                    setState(CefAppState.INITIALIZED);
                    synchronized (initializationListeners_) {
                        isInitialized_ = true;
                        initializationListeners_.forEach(l -> l.stateHasChanged(CefAppState.INITIALIZED));
                        initializationListeners_.clear();
                    }
                    CefLog.Info("Connected to CefServer. JCEF version: %s", getVersion());
                }
            } else {
                preinit(args);
                initialize();
            }
        },
        new NamedThreadExecutor("CefInitialize-thread"));
    }

    private void preinit(String[] args) throws RuntimeException {
            if (OS.isMacintosh()) {
                String[] fixedPathArgs = Startup.fixOSXPathsInArgs(args);
                CefAppHandlerAdapter apph = this;
                if (appHandler_ != null) {
                    if (appHandler_ instanceof CefAppHandlerAdapter)
                        apph = (CefAppHandlerAdapter) appHandler_;
                    else
                        CefLog.Warn("AppHandler is not instance of CefAppHandlerAdapter and path-arguments won't be fixed. Can cause initialization errors.");
                }
                apph.updateArgs(fixedPathArgs);
            }
            Startup.setPathsInSettings(settings_);

        // Start preInit
        AtomicBoolean success = new AtomicBoolean(false);
        if (PREINIT_ON_ANY_THREAD)
            success.set(N_PreInitialize());
        else {
            try {
                SwingUtilities.invokeAndWait(() -> success.set(N_PreInitialize()));
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException("Failed to do JCEF preinit", e);
            }
        }
        if (!success.get()) {
            throw new RuntimeException("Failed to do JCEF preinit");
        }
    }

    // Notifies (in initialization thread) listener that native context has been initialized.
    // When context is already initialized then listener executes immediately.
    public void onInitialization(CefAppStateHandler initListener) {
        onInitialization(initListener, false);
    }

    public void onInitialization(CefAppStateHandler initListener, boolean first) {
        synchronized (initializationListeners_) {
            if (isInitialized_)
                initListener.stateHasChanged(CefAppState.INITIALIZED);
            else {
                if (first)
                    initializationListeners_.addFirst(initListener);
                else
                    initializationListeners_.addLast(initListener);
            }
        }
    }

    /**
     * Assign an AppHandler to CefApp. The AppHandler can be used to evaluate
     * application arguments, to register your own schemes and to hook into the
     * shutdown sequence. See CefAppHandler for more details.
     * <p>
     * This method must be called before CefApp is initialized. CefApp will be
     * initialized automatically if you call createClient() the first time.
     *
     * @param appHandler An instance of CefAppHandler.
     * @throws IllegalStateException in case of CefApp is already initialized
     */
    public static void addAppHandler(CefAppHandler appHandler) throws IllegalStateException {
        if (getState().compareTo(CefAppState.NEW) > 0)
            throw new IllegalStateException("Must be called before CefApp is initialized");
        appHandler_ = appHandler;
    }

    /**
     * Get an instance of this class.
     *
     * @return an instance of this class
     */
    public static synchronized CefApp getInstance() {
        return getInstance(null, null);
    }

    public static synchronized CefApp getInstance(String[] args) {
        return getInstance(args, null);
    }

    public static synchronized CefApp getInstance(CefSettings settings)
            throws UnsatisfiedLinkError {
        return getInstance(null, settings);
    }

    public static synchronized CefApp getInstance(String[] args, CefSettings settings) {
        if (settings != null) {
            if (getState().compareTo(CefAppState.NEW) > 0)
                throw new IllegalStateException("Settings can only be passed to CEF"
                        + " before createClient is called the first time. Current state is " + getState());
        }
        if (self == null) {
            if (getState() == CefAppState.TERMINATED)
                throw new IllegalStateException("CefApp was terminated");
            assert getState() == CefAppState.NONE;
            self = new CefApp(args, settings);
        }
        return self;
    }

    public static synchronized CefApp getInstanceIfAny() throws UnsatisfiedLinkError {
        return self;
    }

    public final void setSettings(CefSettings settings) throws IllegalStateException {
        if (getState().compareTo(CefAppState.NEW) > 0)
            throw new IllegalStateException("Settings can only be passed to CEF"
                    + " before createClient is called the first time. Current state is " + getState());
        settings_ = settings.clone();
    }

    public final CefVersion getVersion() {
        if (IS_REMOTE_ENABLED) {
            // TODO: request remaining params from server
            return new CefVersion(0, "0", 0, 0, 0, 0, 0, 0, 0, 0) {
                @Override
                public String toString() {
                    return "remote_" + CefServer.getVersion();
                }
                @Override
                public String getJcefVersion() {
                    return "remote_" + CefServer.getVersion();
                }
            };
        }
        try {
            return N_GetVersion();
        } catch (UnsatisfiedLinkError ule) {
            CefLog.Error("Failed to get CEF version. %s", ule.getMessage());
        }
        return null;
    }

    public static final boolean isRemoteEnabled() { return IS_REMOTE_ENABLED; }

    /**
     * Returns the current state of CefApp.
     *
     * @return current state.
     */
    public static CefAppState getState() {
        return state_;
    }

    private static void setState(final CefAppState state) {
        if (state.compareTo(state_) < 0) {
            String errMsg = "CefApp: state cannot go backward. Current state " + state_ + ". Proposed state " + state;
            CefLog.Error(errMsg);
            throw new IllegalStateException(errMsg);
        }
        CefLog.Info("CefApp: set state %s", state);
        state_ = state;
        // Execute on the AWT event dispatching thread.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                CefAppHandler handler = appHandler_ == null ? CefApp.self : appHandler_;
                if (handler != null) handler.stateHasChanged(state);
            }
        });
    }

    /**
     * To shutdown the system, it's important to call the dispose
     * method. Calling this method closes all client instances with
     * and all browser instances each client owns. After that the
     * message loop is terminated and CEF is shutdown.
     */
    public synchronized final void dispose() {
        switch (getState()) {
            case NEW:
                // Nothing to do inspite of invalidating the state
                setState(CefAppState.TERMINATED);
                CefApp.self = null;
                break;

            case INITIALIZING:
            case INITIALIZED:
                // (3) Shutdown sequence. Close all clients and continue.
                setState(CefAppState.SHUTTING_DOWN);
                if (clients_.isEmpty()) {
                    finishShutdown();
                } else {
                    // shutdown() will be called from clientWasDisposed() when the last
                    // client is gone.
                    // Use a copy of the HashSet to avoid iterating during modification.
                    CefLog.Debug("CefApp: dispose clients before shutting down");
                    HashSet<CefClient> clients = new HashSet<CefClient>(clients_);
                    for (CefClient c : clients) {
                        CefLog.Debug("CefApp: dispose %s", c);
                        c.dispose();
                    }
                }

                break;

            case NONE:
            case SHUTTING_DOWN:
            case TERMINATED:
                // Ignore shutdown, CefApp is already terminated, in shutdown progress
                // or was never created (shouldn't be possible)
                break;
        }
    }

    /**
     * Creates a new client instance and returns it to the caller.
     * One client instance is responsible for one to many browser
     * instances
     *
     * @return a new client instance
     */
    public synchronized CefClient createClient() {
        if (state_.compareTo(CefAppState.SHUTTING_DOWN) >= 0) {
            String errMsg = "Can't create client in state " + state_;
            CefLog.Error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        CefClient client = new CefClient();
        onInitialization(client, true);
        clients_.add(client);
        return client;
    }

    /**
     * Register a scheme handler factory for the specified |scheme_name| and
     * optional |domain_name|. An empty |domain_name| value for a standard scheme
     * will cause the factory to match all domain names. The |domain_name| value
     * will be ignored for non-standard schemes. If |scheme_name| is a built-in
     * scheme and no handler is returned by |factory| then the built-in scheme
     * handler factory will be called. If |scheme_name| is a custom scheme then
     * also implement the CefApp::OnRegisterCustomSchemes() method in all
     * processes. This function may be called multiple times to change or remove
     * the factory that matches the specified |scheme_name| and optional
     * |domain_name|. Returns false if an error occurs. This function may be
     * called on any thread in the browser process.
     */
    public boolean registerSchemeHandlerFactory(String schemeName, String domainName, CefSchemeHandlerFactory factory) {
        if (IS_REMOTE_ENABLED) {
            CefServer.instance().onConnected(()->{
                RemoteSchemeHandlerFactory rf = RemoteSchemeHandlerFactory.create(factory);
                CefServer.instance().getService().exec(s -> s.SchemeHandlerFactory_Register(schemeName, domainName, rf.thriftId()));
            }, "registerSchemeHandlerFactory", true);
            return true;
        }

        onInitialization(state -> {
            if (!N_RegisterSchemeHandlerFactory(schemeName, domainName, factory))
                CefLog.Error("Can't register scheme [%s:%s]", schemeName, domainName);
        });
        return true;
    }

    /**
     * Clear all registered scheme handler factories. Returns false on error. This
     * function may be called on any thread in the browser process.
     */
    public boolean clearSchemeHandlerFactories() {
        if (IS_REMOTE_ENABLED) {
            CefServer.instance().onConnected(()->{
                CefServer.instance().getService().exec(s -> s.ClearAllSchemeHandlerFactories());
            }, "clearSchemeHandlerFactories", false);
            return true;
        }

        if (!isInitialized_)
            return false;
        return N_ClearSchemeHandlerFactories();
    }

    /**
     * This method is called by a CefClient if it was disposed. This causes
     * CefApp to clean up its list of available client instances. If all clients
     * are disposed, CefApp will be shutdown.
     *
     * @param client the disposed client.
     */
    protected final synchronized void clientWasDisposed(CefClient client) {
        clients_.remove(client);
        synchronized (initializationListeners_) {
            initializationListeners_.remove(client);
        }
        CefLog.Debug("CefApp: client was disposed: %s [clients count %d]", client, clients_.size());
        if (clients_.isEmpty() && getState().compareTo(CefAppState.SHUTTING_DOWN) >= 0) {
            // Shutdown native system.
            finishShutdown();
        }
    }

    private static void testSleep(int ms) {
        if (ms > 0) {
            CefLog.Debug("testSleep %s ms", ms);
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Initialize the context. Can be executed in any thread.
     */
    private void initialize() {
        setState(CefAppState.INITIALIZING);
        testSleep(INIT_TEST_DELAY_MS);

        String logMsg = "Initialize CefApp on " + Thread.currentThread();
        if (settings_.log_severity == CefSettings.LogSeverity.LOGSEVERITY_INFO || settings_.log_severity == CefSettings.LogSeverity.LOGSEVERITY_VERBOSE)
            CefLog.Info(logMsg);
        else
            CefLog.Debug(logMsg);

        CefSettings settings = settings_ != null ? settings_ : new CefSettings();
        Startup.setPathsInSettings(settings);

        boolean success = N_Initialize(appHandler_ == null ? CefApp.this : appHandler_, settings, false);
        if (success) {
            CefLog.Debug("CefApp: native initialization is finished.");
            setState(CefAppState.INITIALIZED);
            synchronized (initializationListeners_) {
                isInitialized_ = true;
                initializationListeners_.forEach(l -> l.stateHasChanged(CefAppState.INITIALIZED));
                initializationListeners_.clear();
            }
            CefLog.Info("version: %s | settings: %s", getVersion(), settings.getDescription());
        } else
            CefLog.Error("CefApp: N_Initialize failed.");
    }

    /**
     * Shut down the context.
     */
    private void finishShutdown() {
        if (IS_REMOTE_ENABLED) {
            CefServer.instance().disconnect();
            synchronized (this) {
                setState(CefAppState.TERMINATED);
                CefApp.self = null;
            }
            return;
        }
        new Thread(() -> {
            // Empiric observation: to avoid crashes we must perform native shutdown in next manner:
            // last client closed -> small pause (to allow CEF finish some bg tasks) -> call CefShutdown
            // This small pause will be in bg thread, see JBR-5822.
            final int sleepBeforeShutdown = Utils.getInteger("JCEF_SLEEP_BEFORE_SHUTDOWN", 999);
            if (sleepBeforeShutdown > 0) {
                try {
                    CefLog.Debug("Sleep before native shutdown for %d ms, thread %s.", sleepBeforeShutdown, Thread.currentThread());
                    Thread.sleep(sleepBeforeShutdown);
                } catch (InterruptedException e) {}
            }

            // Shutdown native CEF.
            CefLog.Info("Perform native shutdown of CEF on thread %s.", Thread.currentThread());
            N_Shutdown();

            synchronized (this) {
                setState(CefAppState.TERMINATED);
                CefApp.self = null;
            }
        }, "CEF-shutdown-thread").start();
    }

    /**
     * Perform a single message loop iteration. Used on all platforms except
     * Windows with windowed rendering.
     */
    public final void doMessageLoopWork(final long delay_ms) {
        if (IS_REMOTE_ENABLED) {
            CefLog.Error("doMessageLoopWork musn't be called in remote mode.");
            return;
        }
        // Execute on the AWT event dispatching thread.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (getState() == CefAppState.TERMINATED) return;

                // The maximum number of milliseconds we're willing to wait between
                // calls to DoMessageLoopWork().
                final long kMaxTimerDelay = 1000 / 30; // 30fps

                if (workTimer_ != null) {
                    workTimer_.stop();
                    workTimer_ = null;
                }

                if (delay_ms <= 0) {
                    // Execute the work immediately.
                    N_DoMessageLoopWork();

                    // Schedule more work later.
                    doMessageLoopWork(kMaxTimerDelay);
                } else {
                    long timer_delay_ms = delay_ms;
                    // Never wait longer than the maximum allowed time.
                    if (timer_delay_ms > kMaxTimerDelay) timer_delay_ms = kMaxTimerDelay;

                    workTimer_ = new Timer((int) timer_delay_ms, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            // Timer has timed out.
                            workTimer_.stop();
                            workTimer_ = null;

                            N_DoMessageLoopWork();

                            // Schedule more work later.
                            doMessageLoopWork(kMaxTimerDelay);
                        }
                    });
                    workTimer_.start();
                }
            }
        });
    }

    /**
     * This method must be called at the beginning of the main() method to perform platform-specific startup
     * initialization. On Linux this initializes Xlib multithreading and on macOS this dynamically loads the
     * CEF framework. Can be executed in any thread.
     *
     * @param args Command-line arguments were perviously used only to get CEF framework path in OSX. Now it's
     *             unused (CEF path is obtained dynamically during startup)
     */
    public static boolean startup(String[] args/*unused*/) {
        startupAsync();
        return true;
    }

    public static void startupAsync() {
        if (IS_REMOTE_ENABLED) {
            ourStartupFeature.complete(null);
            return;
        }

        new NamedThreadExecutor("CefStartup-thread").execute(()->{
            try {
                Startup.loadCefLibrary();
                CefApp.N_Startup(Startup.getPathToFrameworkOSX());
                ourStartupFeature.complete(null);
            } catch (Throwable e) {
                ourStartupFeature.completeExceptionally(e);
            }
        });
    }

    static class NamedThreadExecutor implements Executor {
        private final String name;

        NamedThreadExecutor(String name) {
            this.name = name;
        }

        @Override
        public void execute(Runnable command) {
            new Thread(command, name).start();
        }
    }

    private static native boolean N_Startup(String pathToCefFramework);

    private native boolean N_PreInitialize();

    private native boolean N_Initialize(CefAppHandler appHandler, CefSettings settings, boolean checkThread);

    private native void N_Shutdown();

    private native void N_DoMessageLoopWork();

    private native CefVersion N_GetVersion();

    private native boolean N_RegisterSchemeHandlerFactory(
            String schemeName, String domainName, CefSchemeHandlerFactory factory);

    private native boolean N_ClearSchemeHandlerFactories();
}
