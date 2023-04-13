// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.CefApp.CefAppState;
import org.cef.callback.CefCommandLine;
import org.cef.callback.CefSchemeRegistrar;

/**
 * Implement this interface to provide handler implementations. Methods will be
 * called by the process and/or thread indicated.
 */
public interface CefAppHandler extends CefAppStateHandler {
    /**
     * Provides an opportunity to view and/or modify command-line arguments before
     * processing by CEF and Chromium. The |process_type| value will be empty for
     * the browser process. Be cautious when using this method to modify
     * command-line arguments for non-browser processes as this may result in
     * undefined behavior including crashes.
     * @param process_type type of process (empty for browser process).
     * @param command_line values of the command line.
     */
    public void onBeforeCommandLineProcessing(String process_type, CefCommandLine command_line);

    /**
     * Provides an opportunity to hook into the native shutdown process. This
     * method is invoked if the user tries to terminate the app by sending the
     * corresponding key code (e.g. on Mac: CMD+Q) or something similar. If you
     * want to proceed with the default behavior of the native system, return
     * false. If you want to abort the terminate or if you want to implement your
     * own shutdown sequence return true and do the cleanup on your own.
     * @return false to proceed with the default behavior, true to abort
     * terminate.
     */
    public boolean onBeforeTerminate();

    /**
     * Provides an opportunity to register custom schemes. Do not keep a reference
     * to the |registrar| object. This method is called on the main thread for
     * each process and the registered schemes should be the same across all
     * processes.
     */
    public void onRegisterCustomSchemes(CefSchemeRegistrar registrar);

    // Inherited of CefBrowserProcessHandler
    /**
     * Called on the browser process UI thread immediately after the CEF context
     * has been initialized.
     */
    public void onContextInitialized();

    /**
     * Called from any thread when work has been scheduled for the browser process
     * main (UI) thread. This callback should schedule a
     * CefApp.DoMessageLoopWork() call to happen on the main (UI) thread.
     * |delay_ms| is the requested delay in milliseconds. If |delay_ms| is <= 0
     * then the call should happen reasonably soon. If |delay_ms| is > 0 then the
     * call should be scheduled to happen after the specified delay and any
     * currently pending scheduled call should be cancelled.
     */
    public void onScheduleMessagePumpWork(long delay_ms);

    /**
     * Called before a child process is launched. Will be called on the browser
     * process UI thread when launching a render process and on the browser
     * process IO thread when launching a GPU or plugin process.
     */
    public void onBeforeChildProcessLaunch(String command_line);
}
