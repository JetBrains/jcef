package com.jetbrains.cef.remote;

import com.jetbrains.cef.SharedMemory;

import java.io.IOException;

class WindowsPipe {
    private static final String WIN32_PIPE_PREFIX = "\\\\.\\pipe\\";
    public static final int ERROR_IO_PENDING;
    public static final int ERROR_NO_DATA;
    public static final int ERROR_PIPE_CONNECTED;
    public static final int FILE_ALL_ACCESS;
    public static final int FILE_FLAG_FIRST_PIPE_INSTANCE;
    public static final int FILE_FLAG_OVERLAPPED;
    public static final int FILE_GENERIC_READ;
    public static final int PIPE_ACCESS_DUPLEX;

    static {
        SharedMemory.loadDynamicLib();
        ERROR_IO_PENDING = ERROR_IO_PENDING();
        ERROR_NO_DATA = ERROR_NO_DATA();
        ERROR_PIPE_CONNECTED = ERROR_PIPE_CONNECTED();
        FILE_ALL_ACCESS = FILE_ALL_ACCESS();
        FILE_FLAG_FIRST_PIPE_INSTANCE = FILE_FLAG_FIRST_PIPE_INSTANCE();
        FILE_FLAG_OVERLAPPED = FILE_FLAG_OVERLAPPED();
        FILE_GENERIC_READ = FILE_GENERIC_READ();
        PIPE_ACCESS_DUPLEX = PIPE_ACCESS_DUPLEX();
    }

    static String normalizePipePath(String path) {
        return path.startsWith(WIN32_PIPE_PREFIX) ? path : WIN32_PIPE_PREFIX + path;
    }
    static native long CreateNamedPipe(
            String lpName,
            int dwOpenMode,
            int dwPipeMode,
            int nMaxInstances,
            int nOutBufferSize,
            int nInBufferSize,
            int nDefaultTimeOut,
            int lpSecurityAttributes) throws IOException;

    static native long OpenFile(String pipeName) throws IOException; // for client connection

    static native int ConnectNamedPipe(long handlePointer, long overlappedPointer);

    static native boolean DisconnectNamedPipe(long handlePointer);

    static native int read(
            long waitable, long hFile, byte[] buffer, int offset, int len)
            throws IOException;

    static native void write(
            long waitablePointer, long hFilePointer, byte[] lpBuffer, int offset, int len)
            throws IOException;

    static native boolean CloseHandle(long pointer);
    static native boolean GetOverlappedResult(long handlePointer, long overlappedPointer);
    static native long CreateEvent(boolean bManualReset, boolean bInitialState, String lpName) throws IOException;
    public static native int GetLastError();
    static native long NewOverlapped(long handle);
    static native boolean FlushFileBuffers(long handle);
    static native void DeleteOverlapped(long overlapped);

    // Constants:
    private static native int ERROR_IO_PENDING();
    private static native int ERROR_NO_DATA();
    private static native int ERROR_PIPE_CONNECTED();
    private static native int FILE_ALL_ACCESS();
    private static native int FILE_FLAG_FIRST_PIPE_INSTANCE();
    private static native int FILE_FLAG_OVERLAPPED();
    private static native int FILE_GENERIC_READ();
    private static native int PIPE_ACCESS_DUPLEX();
}
