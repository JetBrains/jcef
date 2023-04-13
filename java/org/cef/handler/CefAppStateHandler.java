package org.cef.handler;

import org.cef.CefApp;

public interface CefAppStateHandler {
    /**
     * Implement this method to get state changes of the CefApp.
     * See {@link CefApp.CefAppState} for a complete list of possible states.
     *
     * For example, this method can be used e.g. to get informed if CefApp has
     * completed its initialization or its shutdown process.
     *
     * @param state The current state of CefApp.
     */
    public void stateHasChanged(CefApp.CefAppState state);
}
