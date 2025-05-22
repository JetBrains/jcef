package com.jetbrains.cef.remote;

import com.jetbrains.cef.SharedMemory;
import com.jetbrains.cef.remote.thrift_codegen.CefKeyEventAttributes;

import java.awt.event.KeyEvent;

public class PlatformUtils {
    static {
        SharedMemory.loadDynamicLib();
    }

    public static native CefKeyEventAttributes getCefKeyEventAttributes(KeyEvent e);
}
