package com.jetbrains.cef.remote;

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

    private native long openSharedSegment(String sid);
    private native long getPointer(long segment, long handle);
    private native void closeSharedSegment(long segment);
}
