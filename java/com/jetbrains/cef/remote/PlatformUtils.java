package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.CefKeyEvent;

import java.awt.event.KeyEvent;

public class PlatformUtils {
    static {
        System.loadLibrary("shared_mem_helper");
    }

    static native CefKeyEvent toCefKeyEvent(KeyEvent e);
}
