package com.jetbrains.cef.remote;

import java.io.IOException;

class Win32Pipe {
    public static long CreateNamedPipe(
            String lpName,
            int dwOpenMode,
            int dwPipeMode,
            int nMaxInstances,
            int nOutBufferSize,
            int nInBufferSize,
            int nDefaultTimeOut,
            int lpSecurityAttributes,
            int securityLevel)
            throws IOException {
        return CreateNamedPipeNative(
                        lpName,
                        dwOpenMode,
                        dwPipeMode,
                        nMaxInstances,
                        nOutBufferSize,
                        nInBufferSize,
                        nDefaultTimeOut,
                        lpSecurityAttributes,
                        securityLevel);
    }
    static native long CreateNamedPipeNative(
            String lpName,
            int dwOpenMode,
            int dwPipeMode,
            int nMaxInstances,
            int nOutBufferSize,
            int nInBufferSize,
            int nDefaultTimeOut,
            int lpSecurityAttributes,
            int securityLevel)
            throws IOException;

    public static long CreateFile(String pipeName) throws IOException {
        return CreateFileNative(pipeName);
    }

    static native long CreateFileNative(String pipeName) throws IOException;

    public static int ConnectNamedPipe(long hNamedPipe, long lpOverlapped) {
        return ConnectNamedPipeNative(hNamedPipe, lpOverlapped);
    }

    static native int ConnectNamedPipeNative(long handlePointer, long overlappedPointer);

    static native boolean DisconnectNamedPipe(long handlePointer);

    public static int read(
            long waitable,
            long hFile,
            byte[] buffer,
            int offset,
            int len,
            boolean requireStrictLength)
            throws IOException {
        return readNative(
                waitable,
                hFile,
                buffer,
                offset,
                len,
                requireStrictLength);
    }

    static native int readNative(
            long waitable, long hFile, byte[] buffer, int offset, int len, boolean requireStrictLength)
            throws IOException;

    public static void write(long waitable, long hFile, byte[] lpBuffer, int offset, int len)
            throws IOException {
        writeNative(waitable, hFile, lpBuffer, offset, len);
    }

    static native void writeNative(
            long waitablePointer, long hFilePointer, byte[] lpBuffer, int offset, int len)
            throws IOException;

    public static boolean CloseHandle(long handle) {
        return CloseHandleNative(handle);
    }

    static native boolean CloseHandleNative(long pointer);

    public static boolean GetOverlappedResult(long hFile, long lpOverlapped) {
        return GetOverlappedResultNative(hFile, lpOverlapped);
    }

    static native boolean GetOverlappedResultNative(long handlePointer, long overlappedPointer);

    static native boolean CancelIoEx(long pointer);

    public static long CreateEvent(boolean bManualReset, boolean bInitialState, String lpName)
            throws IOException {
        return CreateEventNative(bManualReset, bInitialState, lpName);
    }

    static native long CreateEventNative(boolean bManualReset, boolean bInitialState, String lpName);

    public static native int GetLastError();

    public static long NewOverlapped(long hEvent) {
        return NewOverlappedNative(hEvent);
    }

    static native long NewOverlappedNative(long handle);

    public static void DeleteOverlapped(long overlapped) {
        DeleteOverlappedNative(overlapped);
    }

    static native boolean FlushFileBuffersNative(long handle);

    public static boolean FlushFileBuffers(long handle) {
        return FlushFileBuffersNative(handle);
    }

    static native void DeleteOverlappedNative(long overlapped);

    static native String getErrorMessage(int errorCode);

    // Constants:
    public static native int ERROR_IO_PENDING();

    public static native int ERROR_NO_DATA();

    public static native int ERROR_PIPE_CONNECTED();

    public static native int FILE_ALL_ACCESS();

    public static native int FILE_FLAG_FIRST_PIPE_INSTANCE();

    public static native int FILE_FLAG_OVERLAPPED();

    public static native int FILE_GENERIC_READ();

    public static native int GENERIC_READ();

    public static native int GENERIC_WRITE();

    public static native int PIPE_ACCESS_DUPLEX();
}
