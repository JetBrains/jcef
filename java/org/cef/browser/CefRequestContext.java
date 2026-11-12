// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import com.jetbrains.cef.remote.network.RemoteRequestContext;
import org.cef.CefApp;
import org.cef.callback.CefCallback;
import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefNativeAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestContextHandler;
import org.cef.network.CefCookieManager;
import org.cef.security.CefSSLInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * A request context provides request handling for a set of related browser
 * objects. A request context is specified when creating a new browser object
 * via the CefClient.createBrowser method. Browser objects with different
 * request contexts will never be hosted in the same render process. Browser
 * objects with the same request context may or may not be hosted in the same
 * render process depending on the process model. Browser objects created
 * indirectly via the JavaScript window.open function or targeted links will
 * share the same render process and the same request context as the source
 * browser. When running in single-process mode there is only a single render
 * process (the main process) and so all browsers created in single-process mode
 * will share the same request context. This will be the first request context
 * passed into the CefClient.createBrowser method and all other request
 * context objects will be ignored.
 */
public abstract class CefRequestContext extends CefNativeAdapter {
    // This CTOR can't be called directly. Call method create() instead.
    protected CefRequestContext() {}

    /**
     * Returns the global context object.
     */
    public static final CefRequestContext getGlobalContext() {
        if (CefApp.isRemoteEnabled())
            return new RemoteRequestContext(CefApp.getInstance().getServer());

        return CefRequestContext_N.getGlobalContextNative();
    }

    /**
     * Creates a new context object with the specified handler and default
     * settings (empty cache path, i.e. in-memory "incognito" storage).
     */
    public static final CefRequestContext createContext(CefRequestContextHandler handler) {
        return createContext((CefRequestContextSettings) null, handler);
    }

    /**
     * Creates a new context object with the specified |settings| and optional
     * |handler|. If |settings| is null the default values are used.
     * <p>
     * Note that {@link CefRequestContextSettings#cache_path} must be either
     * empty (in-memory storage) or an absolute path that is equal to or a child
     * directory of CefSettings.root_cache_path. Two contexts created with the
     * same non-empty cache path share the same storage (see
     * {@link #isSharingWith(CefRequestContext)}).
     */
    public static final CefRequestContext createContext(
            CefRequestContextSettings settings, CefRequestContextHandler handler) {
        if (CefApp.isRemoteEnabled())
            return new RemoteRequestContext(CefApp.getInstance().getServer(), settings, handler);

        return CefRequestContext_N.createNative(settings, handler);
    }

    /**
     * Creates a new context object that shares storage (cookies, cache, local
     * storage, etc.) with |other| and uses an optional |handler|. The storage
     * lives as long as at least one context object (or browser) that refers to
     * it is alive. Returns null if |other| is null or invalid.
     */
    public static final CefRequestContext createContext(
            CefRequestContext other, CefRequestContextHandler handler) {
        if (other == null)
            return null;

        if (CefApp.isRemoteEnabled())
            return new RemoteRequestContext(CefApp.getInstance().getServer(), other, handler);

        return CefRequestContext_N.createNative(other, handler);
    }

    public abstract void dispose();

    /**
     * Returns true if this object is pointing to the same context as |other|
     * object.
     */
    public boolean isSame(CefRequestContext other) {
        return this == other;
    }

    /**
     * Returns true if this object is sharing the same storage as |other| object.
     */
    public boolean isSharingWith(CefRequestContext other) {
        return isSame(other);
    }

    /**
     * Returns true if this object is the global context.
     */
    public abstract boolean isGlobal();

    /**
     * Returns the cache path for this object. If empty an "incognito mode"
     * in-memory cache is being used.
     */
    public String getCachePath() {
        return "";
    }

    /**
     * Returns the cookie manager for this object (null if not supported by the
     * implementation). In the out-of-process mode the returned object becomes
     * usable after the connection with the server is established (see
     * CefApp#onInitialization).
     */
    public CefCookieManager getCookieManager() {
        return null;
    }

    /**
     * Returns the handler for this context if any.
     */
    public abstract CefRequestContextHandler getHandler();

    /**
     * Returns true if a preference with the specified |name| exists.
     * <p>
     * This method must be called on the browser process UI thread, otherwise it will always return
     * false. It is easiest to ensure the correct calling thread by using a callback method invoked
     * by the browser process UI thread, such as CefLifeSpanHandler.onAfterCreated(CefBrowser), to
     * configure the preferences.
     */
    public abstract boolean hasPreference(String name);

    /**
     * Returns the value for the preference with the specified |name|. Returns
     * NULL if the preference does not exist.
     * This method must be called on the browser process UI thread, otherwise it will always return
     * null.
     */
    public abstract Object getPreference(String name);

    /**
     * Returns all preferences as a dictionary. If |includeDefaults| is true then
     * preferences currently at their default value will be included. The returned
     * object can be modified but modifications will not persist. This method must
     * be called on the browser process UI thread, otherwise it will always return null.
     */
    public abstract Map<String, Object> getAllPreferences(boolean includeDefaults);

    /**
     * Returns true if the preference with the specified |name| can be modified
     * using setPreference. As one example preferences set via the command-line
     * usually cannot be modified. This method must be called on the browser
     * process UI thread, otherwise it will always return false.
     */
    public abstract boolean canSetPreference(String name);

    /**
     * Set the |value| associated with preference |name|. Returns null if the
     * value is set successfully, an error string otherwise. If |value| is NULL the
     * preference will be restored to its default value. If setting the preference
     * fails then a detailed description of the problem will be returned.
     * This method must be called on the browser process UI thread, otherwise it will always return
     * an error string.
     */
    public abstract String setPreference(String name, Object value);

    /**
     * Clears all certificate exceptions that were added as part of handling
     * {@link org.cef.handler.CefRequestHandler#onCertificateError(
     * CefBrowser, CefLoadHandler.ErrorCode, String, CefSSLInfo, CefCallback)}.
     * If you call this it is recommended that you also call CloseAllConnections()
     * or you risk not being prompted again for server certificates if you reconnect
     * quickly. If {@code callback} is non-null it will be executed on the UI thread
     * after completion.
     *
     * @param callback optional completion callback
     */
    public abstract void ClearCertificateExceptions(CefCompletionCallback callback);

    /**
     * Clears all active and idle connections that Chromium currently has.
     * If {@code callback} is non-null it will be executed on the UI thread after completion.
     *
     * @param callback optional completion callback
     */
    public abstract void CloseAllConnections(CefCompletionCallback callback);

    /**
     * Clears all HTTP cache entries for this context (does not affect cookies,
     * local storage or other DOM storage).
     * If {@code callback} is non-null it will be executed on the UI thread after completion.
     *
     * @param callback optional completion callback
     */
    public void clearHttpCache(CefCompletionCallback callback) {
        if (callback != null) callback.onComplete();
    }

    /**
     * Clears all HTTP authentication credentials that were added as part of
     * handling {@link org.cef.handler.CefRequestHandler#getAuthCredentials}.
     * If {@code callback} is non-null it will be executed on the UI thread after completion.
     *
     * @param callback optional completion callback
     */
    public void clearHttpAuthCredentials(CefCompletionCallback callback) {
        if (callback != null) callback.onComplete();
    }
}
