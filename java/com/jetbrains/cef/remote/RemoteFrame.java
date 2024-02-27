package com.jetbrains.cef.remote;

import org.cef.browser.CefFrame;

public class RemoteFrame implements CefFrame {
    @Override
    public void dispose() {
    }

    @Override
    public String getIdentifier() {
        return "RemoteFrame_";
    }

    @Override
    public String getURL() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isMain() {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public CefFrame getParent() {
        return null;
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {

    }

    @Override
    public void undo() {

    }

    @Override
    public void redo() {

    }

    @Override
    public void cut() {

    }

    @Override
    public void copy() {

    }

    @Override
    public void paste() {

    }

    @Override
    public void delete() {

    }

    @Override
    public void selectAll() {

    }
}
