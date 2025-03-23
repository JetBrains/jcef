package com.jetbrains.cef.remote.browser;

import org.cef.browser.CefRegistration;

public class RemoteRegistration extends CefRegistration {
    private RemoteRegistrationImpl myImpl;

    public RemoteRegistration(RemoteRegistrationImpl impl) {
        super();
        myImpl = impl;
    }

    @Override
    public void dispose() {
        myImpl.disposeOnServer();
    }
}