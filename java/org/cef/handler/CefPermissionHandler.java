// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefMediaAccessCallback;

/**
 * Implement this interface to handle events related to media access permission
 * requests. The methods of this class will be called on the UI thread.
 */
public interface CefPermissionHandler {
    /**
     * Called when a page requests permission to access media.
     * Return true and call CefMediaAccessCallback::Continue() either in this method or at a later
     * time to continue or cancel the request. Return false to cancel the request immediately.
     * @param browser The browser generating the event.
     * @param frame The corresponding frame.
     * @param requesting_url The URL requesting permission.
     * @param requested_permissions The bitmask of requested permissions.
     * @param callback execute callback to provide media permissions.
     */
    boolean onRequestMediaAccessPermission(
            CefBrowser browser,
            CefFrame frame,
            String requesting_url,
            int requested_permissions,
            CefMediaAccessCallback callback);
}
