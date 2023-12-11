package com.jetbrains.cef.remote;

import org.cef.misc.Utils;

import java.nio.ByteBuffer;

public class SharedMemory {
    private static final String ALT_MEM_HELPER_PATH = Utils.getString("ALT_MEM_HELPER_PATH");
    public final String mname;
    public final long boostHandle;

    final private long mySegment;
    final private long myPtr;

    static {
        if ("".equals(ALT_MEM_HELPER_PATH))
            System.loadLibrary("shared_mem_helper");
        else
            System.load(ALT_MEM_HELPER_PATH);
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

    public ByteBuffer wrap(int size) {
        return wrapNativeMem(myPtr, size);
    }

    // Helper method (for creating BufferedImage from native raster)
    private static native ByteBuffer wrapNativeMem(long pdata, int length);
    
    public static class WithRaster extends SharedMemory {
        private int myWidth;
        private int myHeight;
        private int myDirtyRectsCount;

        public WithRaster(String sharedMemName, long boostHandle) {
            super(sharedMemName, boostHandle);
        }

        public ByteBuffer wrapRaster() {
            return wrapNativeMem(getPtr(), myWidth * myHeight * 4);
        }
        public ByteBuffer wrapRects() {
            return wrapNativeMem(getPtr() + myWidth * myHeight * 4, myDirtyRectsCount * 4 * 4);
        }

        public int getWidth() {
            return myWidth;
        }

        public void setWidth(int width) {
            this.myWidth = width;
        }

        public int getHeight() {
            return myHeight;
        }

        public void setHeight(int height) {
            this.myHeight = height;
        }

        public int getDirtyRectsCount() {
            return myDirtyRectsCount;
        }

        public void setDirtyRectsCount(int dirtyRectsCount) {
            this.myDirtyRectsCount = dirtyRectsCount;
        }
    }

    //
    // Private native API
    //
    private static native long openSharedSegment(String sid);
    private static native long getPointer(long segment, long handle);
    private static native void closeSharedSegment(long segment);
}
