package com.jetbrains.cef;

import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.nio.ByteBuffer;

public class SharedMemory {
    private static final String ALT_MEM_HELPER_PATH = Utils.getString("ALT_MEM_HELPER_PATH");
    public final String mname;
    public final long boostHandle;
    public long lasUsedMs = 0;

    final private long mySegment;
    final private long myPtr;
    private volatile boolean myClosed = false;

    final private long myMutex;

    static {
        loadDynamicLib();
    }

    public static void loadDynamicLib() {
        try {
            if (ALT_MEM_HELPER_PATH == null || ALT_MEM_HELPER_PATH.isEmpty())
                System.loadLibrary("shared_mem_helper");
            else
                System.load(ALT_MEM_HELPER_PATH.trim());
        } catch (UnsatisfiedLinkError e) {
            CefLog.Error("Can't load shared_mem_helper, exception: %s", e.getMessage());
        }
    }

    public SharedMemory(String sharedMemName, long boostHandle) {
        mname = sharedMemName;
        this.boostHandle = boostHandle;
        this.mySegment = openSharedSegment(sharedMemName);
        this.myPtr = getPointer(mySegment, boostHandle);

        myMutex = openSharedMutex(sharedMemName);
    }

    public void lock() {
        lockSharedMutex(myMutex);
    }

    public void unlock() {
        unlockSharedMutex(myMutex);
    }

    public long getPtr() {
        return myPtr;
    }

    public boolean isClosed() { return myClosed; }

    synchronized
    public void close() {
        if (myClosed)
            return;
        myClosed = true;
        closeSharedSegment(mySegment);
        closeSharedMutex(myMutex);
    }

    public ByteBuffer wrap(int size) {
        return wrapNativeMem(myPtr, size);
    }

    public int readInt() {
        return readInt(myPtr, 0);
    }

    public int readInt(int offset) {
        return readInt(myPtr, offset);
    }

    public int readByte(int offset) {
        return readByte(myPtr, offset);
    }

    // Helper method (for creating BufferedImage from native raster)
    private static native ByteBuffer wrapNativeMem(long pdata, int length);

    private static native int readInt(long pdata, int offset);

    private static native int readByte(long pdata, int offset);
    
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
            return wrapNativeMem(getPtr() + getRectsOffset(), myDirtyRectsCount * 4 * 4);
        }

        public int getRectsOffset() { return myWidth * myHeight * 4; }

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

    private static native long openSharedMutex(String uid);
    private static native void lockSharedMutex(long mutex);
    private static native void unlockSharedMutex(long mutex);
    private static native void closeSharedMutex(long mutex);
}
