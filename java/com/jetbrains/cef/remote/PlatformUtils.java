package com.jetbrains.cef.remote;

import com.jetbrains.cef.SharedMemory;
import com.jetbrains.cef.remote.thrift_codegen.CefKeyEventAttributes;

import java.awt.event.KeyEvent;

public class PlatformUtils {
    static {
        SharedMemory.loadDynamicLib();
    }

    public static native CefKeyEventAttributes getCefKeyEventAttributes(KeyEvent e);

    public static native long getMtlTexture(long pSurfaceRef);
    public static native void releaseMtlTextureRef(long pMtlTexture);

    public static native void retainIOSurfaceRef(long pSurfaceRef);
    public static native void releaseIOSurfaceRef(long pSurfaceRef);

    public static native long getIOSurfaceRef(long surfaceId);
}
