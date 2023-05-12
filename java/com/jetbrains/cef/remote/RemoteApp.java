package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.CustomScheme;
import org.cef.callback.CefSchemeRegistrar;

import java.util.ArrayList;
import java.util.List;

public class RemoteApp {
    RemoteApp() {}

    // NOTES:
    // 1. Additional arguments are passed on connection creation (see native peer)
    // 2. For the current JBCefApp implementation we needs only next 2 methods.

    public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        // Override if necessary
    }

    public void onContextInitialized() {
        // Override if necessary
    }

    List<CustomScheme> getAllRegisteredCustomSchemes() {
        final List<CustomScheme> result = new ArrayList<>();
        CefSchemeRegistrar collector = new CefSchemeRegistrar() {
            @Override
            public boolean addCustomScheme(String schemeName, boolean isStandard, boolean isLocal, boolean isDisplayIsolated, boolean isSecure, boolean isCorsEnabled, boolean isCspBypassing, boolean isFetchEnabled) {
                CustomScheme cs = new CustomScheme();
                cs.schemeName = schemeName;
                cs.options = 0;
                if (isStandard) cs.options |= 1 << 0;
                if (isLocal) cs.options |= 1 << 1;
                if (isDisplayIsolated) cs.options |= 1 << 2;
                if (isSecure) cs.options |= 1 << 3;
                if (isCorsEnabled) cs.options |= 1 << 4;
                if (isCspBypassing) cs.options |= 1 << 5;
                if (isFetchEnabled) cs.options |= 1 << 6;
                result.add(cs);
                return false;
            }
        };
        onRegisterCustomSchemes(collector);
        return result;
    }
}
