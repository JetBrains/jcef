package com.jetbrains.cef.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Win32NamedPipeServerSocket extends ServerSocket {
    final static int PIPE_ACCESS_DUPLEX = 3;
    final static int PIPE_UNLIMITED_INSTANCES = 255;
    final static int FILE_FLAG_FIRST_PIPE_INSTANCE = 524288;

    private static final String WIN32_PIPE_PREFIX = "\\\\.\\pipe\\";
    private static final int BUFFER_SIZE = 65535;
    private final LinkedBlockingQueue<Long> openHandles;
    private final LinkedBlockingQueue<Long> connectedHandles;
    private final Win32NamedPipeSocket.CloseCallback closeCallback;
    private final String path;
    private final int maxInstances;
    private final long lockHandle;
    private final boolean requireStrictLength;
    private final int securityLevel;

    public Win32NamedPipeServerSocket(String path) throws IOException {
        this(PIPE_UNLIMITED_INSTANCES, path);
    }

    public Win32NamedPipeServerSocket(String path, int securityLevel)
            throws IOException {
        this(
                PIPE_UNLIMITED_INSTANCES,
                path,
                Win32NamedPipeSocket.DEFAULT_REQUIRE_STRICT_LENGTH,
                securityLevel);
    }

    /**
     * The doc for InputStream#read(byte[] b, int off, int len) states that "An attempt is made to
     * read as many as len bytes, but a smaller number may be read." However, using
     * requireStrictLength, Win32NamedPipeSocketInputStream can require that len matches up exactly
     * the number of bytes to read.
     */
    @Deprecated
    public Win32NamedPipeServerSocket(String path, boolean requireStrictLength) throws IOException {
        this(PIPE_UNLIMITED_INSTANCES, path, requireStrictLength);
    }

    public Win32NamedPipeServerSocket(int maxInstances, String path) throws IOException {
        this(maxInstances, path, Win32NamedPipeSocket.DEFAULT_REQUIRE_STRICT_LENGTH);
    }

    /**
     * The doc for InputStream#read(byte[] b, int off, int len) states that "An attempt is made to
     * read as many as len bytes, but a smaller number may be read." However, using
     * requireStrictLength, NGWin32NamedPipeSocketInputStream can require that len matches up exactly
     * the number of bytes to read.
     */
    public Win32NamedPipeServerSocket(int maxInstances, String path, boolean requireStrictLength)
            throws IOException {
        this(maxInstances, path, requireStrictLength, 2/*LOGON_DACL*/);
    }
    /**
     * The doc for InputStream#read(byte[] b, int off, int len) states that "An attempt is made to
     * read as many as len bytes, but a smaller number may be read." However, using
     * requireStrictLength, NGWin32NamedPipeSocketInputStream can require that len matches up exactly
     * the number of bytes to read.
     */
    public Win32NamedPipeServerSocket(
            int maxInstances, String path, boolean requireStrictLength, int securityLevel)
            throws IOException {
        this.securityLevel = securityLevel;
        this.openHandles = new LinkedBlockingQueue<>();
        this.connectedHandles = new LinkedBlockingQueue<>();
        this.closeCallback =
                handle -> {
                    if (connectedHandles.remove(handle)) {
                        closeConnectedPipe(handle);
                    }
                    if (openHandles.remove(handle)) {
                        closeOpenPipe(handle);
                    }
                };
        this.maxInstances = maxInstances;
        this.requireStrictLength = requireStrictLength;
        if (!path.startsWith(WIN32_PIPE_PREFIX)) {
            this.path = WIN32_PIPE_PREFIX + path;
        } else {
            this.path = path;
        }
        String lockPath = this.path + "_lock";
        try {
            lockHandle =
                    Win32Pipe.CreateNamedPipe(
                            lockPath,
                            Win32Pipe.FILE_FLAG_FIRST_PIPE_INSTANCE() | Win32Pipe.PIPE_ACCESS_DUPLEX(),
                            0,
                            1,
                            BUFFER_SIZE,
                            BUFFER_SIZE,
                            0,
                            Win32Pipe.FILE_GENERIC_READ(),
                            securityLevel);
        } catch (final IOException e) {
            throw new IOException(
                    String.format(
                            "Could not create lock for %s, error %d", lockPath, Win32Pipe.GetLastError()));
        }
        if (!Win32Pipe.DisconnectNamedPipe(lockHandle)) {
            throw new IOException(String.format("Could not disconnect lock %d", Win32Pipe.GetLastError()));
        }
    }

    public void bind(SocketAddress endpoint) throws IOException {
        throw new IOException("Win32 named pipes do not support bind(), pass path to constructor");
    }

    public Socket accept() throws IOException {
        long handle;
        try {
            handle =
                    Win32Pipe.CreateNamedPipe(
                            path,
                            Win32Pipe.PIPE_ACCESS_DUPLEX() | Win32Pipe.FILE_FLAG_OVERLAPPED(),
                            0,
                            maxInstances,
                            BUFFER_SIZE,
                            BUFFER_SIZE,
                            0,
                            Win32Pipe.FILE_ALL_ACCESS(),
                            securityLevel);
        } catch (final IOException e) {
            throw new IOException(
                    String.format("Could not create named pipe, error %d", Win32Pipe.GetLastError()));
        }
        openHandles.add(handle);

        long connWaitable = Win32Pipe.CreateEvent(true, false, null);
        long overlapped = Win32Pipe.NewOverlapped(connWaitable);
        try {

            int connectError = Win32Pipe.ConnectNamedPipe(handle, overlapped);
            if (connectError == -1) {
                openHandles.remove(handle);
                connectedHandles.add(handle);
                return new Win32NamedPipeSocket(handle, closeCallback, requireStrictLength);
            }

            if (connectError == Win32Pipe.ERROR_PIPE_CONNECTED()) {
                openHandles.remove(handle);
                connectedHandles.add(handle);
                return new Win32NamedPipeSocket(handle, closeCallback, requireStrictLength);
            } else if (connectError == Win32Pipe.ERROR_NO_DATA()) {
                // Client has connected and disconnected between CreateNamedPipe() and ConnectNamedPipe()
                // connection is broken, but it is returned it avoid loop here.
                // Actual error will happen for NGSession when it will try to read/write from/to pipe
                return new Win32NamedPipeSocket(handle, closeCallback, requireStrictLength);
            } else if (connectError == Win32Pipe.ERROR_IO_PENDING()) {
                if (!Win32Pipe.GetOverlappedResult(handle, overlapped)) {
                    openHandles.remove(handle);
                    closeOpenPipe(handle);
                    throw new IOException(
                            "GetOverlappedResult() failed for connect operation: " + Win32Pipe.GetLastError());
                }
                openHandles.remove(handle);
                connectedHandles.add(handle);
                return new Win32NamedPipeSocket(handle, closeCallback, requireStrictLength);
            } else {
                throw new IOException("ConnectNamedPipe() failed with: " + connectError);
            }
        } finally {
            Win32Pipe.DeleteOverlapped(overlapped);
            Win32Pipe.CloseHandle(connWaitable);
        }
    }

    public void close() throws IOException {
        try {
            List<Long> handlesToClose = new ArrayList<>();
            openHandles.drainTo(handlesToClose);
            for (long handle : handlesToClose) {
                closeOpenPipe(handle);
            }

            List<Long> handlesToDisconnect = new ArrayList<>();
            connectedHandles.drainTo(handlesToDisconnect);
            for (long handle : handlesToDisconnect) {
                closeConnectedPipe(handle);
            }
        } finally {
            Win32Pipe.CloseHandle(lockHandle);
        }
    }

    private void closeOpenPipe(long handle) throws IOException {
        Win32Pipe.CancelIoEx(handle);
        Win32Pipe.CloseHandle(handle);
    }

    private void closeConnectedPipe(long handle) throws IOException {
        Win32Pipe.FlushFileBuffers(handle);
        Win32Pipe.DisconnectNamedPipe(handle);
        Win32Pipe.CloseHandle(handle);
    }
}