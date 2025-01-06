package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.CefKeyEventAttributes;

import java.awt.event.KeyEvent;

public class PlatformUtils {
    static {
        System.loadLibrary("shared_mem_helper");
    }

    public static native CefKeyEventAttributes getCefKeyEventAttributes(KeyEvent e);
}
