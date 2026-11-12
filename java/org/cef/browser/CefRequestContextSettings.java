// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

/**
 * Request context initialization settings. Specify null or false to get the
 * recommended default values.
 */
public class CefRequestContextSettings {
    /**
     * The directory where cache data for this request context will be stored on
     * disk. If this value is non-empty then it must be an absolute path that is
     * either equal to or a child directory of CefSettings.root_cache_path. If
     * this value is empty then browsers will be created in "incognito mode"
     * where in-memory caches are used for storage and no profile-specific data
     * is persisted to disk (installation-specific data will still be persisted
     * in root_cache_path). HTML5 databases such as localStorage will only
     * persist across sessions if a cache path is specified. To share the global
     * browser cache and related configuration set this value to match the
     * CefSettings.cache_path value.
     * <p>
     * NOTE: an invalid path (relative, or not under root_cache_path) is not an
     * error for CEF: it silently falls back to in-memory storage. Use
     * {@link CefRequestContext#getCachePath()} to verify the effective value.
     */
    public String cache_path = null;

    /**
     * To persist session cookies (cookies without an expiry date or validity
     * interval) by default when using the global cookie manager set this value
     * to true. Session cookies are generally intended to be transient and
     * most Web browsers do not persist them. Can be set globally using the
     * CefSettings.persist_session_cookies value. This value will be ignored if
     * |cache_path| is empty or if it matches the CefSettings.cache_path value.
     */
    public boolean persist_session_cookies = false;

    /**
     * Comma delimited ordered list of language codes without any whitespace that
     * will be used in the "Accept-Language" HTTP request header and
     * "navigator.language" JS attribute. Can be set globally using the
     * CefSettings.accept_language_list value. If all values are empty then
     * "en-US,en" will be used. This value will be ignored if |cache_path|
     * matches the CefSettings.cache_path value.
     */
    public String accept_language_list = null;

    /**
     * Comma delimited list of schemes supported by the associated
     * CefCookieManager. If |cookieable_schemes_exclude_defaults| is false
     * the default schemes ("http", "https", "ws" and "wss") will also be
     * supported. Not specifying a |cookieable_schemes_list| value and setting
     * |cookieable_schemes_exclude_defaults| to true will disable all loading
     * and saving of cookies. These values will be ignored if |cache_path|
     * matches the CefSettings.cache_path value.
     */
    public String cookieable_schemes_list = null;
    public boolean cookieable_schemes_exclude_defaults = false;

    public CefRequestContextSettings() {}

    @Override
    public CefRequestContextSettings clone() {
        CefRequestContextSettings tmp = new CefRequestContextSettings();
        tmp.cache_path = cache_path;
        tmp.persist_session_cookies = persist_session_cookies;
        tmp.accept_language_list = accept_language_list;
        tmp.cookieable_schemes_list = cookieable_schemes_list;
        tmp.cookieable_schemes_exclude_defaults = cookieable_schemes_exclude_defaults;
        return tmp;
    }

    @Override
    public String toString() {
        return "CefRequestContextSettings{cache_path=" + cache_path
                + ", persist_session_cookies=" + persist_session_cookies
                + ", accept_language_list=" + accept_language_list
                + ", cookieable_schemes_list=" + cookieable_schemes_list
                + ", cookieable_schemes_exclude_defaults=" + cookieable_schemes_exclude_defaults + "}";
    }
}
