package com.jetbrains.cef.remote;

import java.nio.ByteBuffer;

public class SharedMemory {
    public final String mname;
    public final long boostHandle;

    final private long mySegment;
    final private long myPtr;

    static {
        System.loadLibrary("shared_mem_helper");
    }

    public SharedMemory(String sharedMemName, long boostHandle) {
        mname = sharedMemName;
        this.boostHandle = boostHandle;
        this.mySegment = openSharedSegment(sharedMemName);
        this.myPtr = getPointer(mySegment, boostHandle);
    }

    public long getPtr() {
        return myPtr;
    }

    public void close() {
        closeSharedSegment(mySegment);
    }

    // Helper method (for creating BufferedImage from native raster)
    public static native ByteBuffer wrapNativeMem(long pdata, int length);

    //
    // Private native API
    //
    private static native long openSharedSegment(String sid);
    private static native long getPointer(long segment, long handle);
    private static native void closeSharedSegment(long segment);
}
