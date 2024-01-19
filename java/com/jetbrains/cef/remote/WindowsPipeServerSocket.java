package com.jetbrains.cef.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class WindowsPipeServerSocket extends ServerSocket {
    final static int PIPE_UNLIMITED_INSTANCES = 255;
    private static final int BUFFER_SIZE = 65535;
    private final LinkedBlockingQueue<Long> myOpenHandles;
    private final LinkedBlockingQueue<Long> myConnectedHandles;
    private final String myPath;
    private final int myMaxInstances;
    private Consumer<Long> myCloseCallback;

    public WindowsPipeServerSocket(String path) throws IOException {
        myOpenHandles = new LinkedBlockingQueue<>();
        myConnectedHandles = new LinkedBlockingQueue<>();
        myCloseCallback =
                handle -> {
                    if (myConnectedHandles.remove(handle)) {
                        closeConnectedPipe(handle);
                    }
                    if (myOpenHandles.remove(handle)) {
                        WindowsPipe.CloseHandle(handle);
                    }
                };
        myMaxInstances = PIPE_UNLIMITED_INSTANCES;
        myPath = WindowsPipe.normalizePipePath(path);
    }

    public void bind(SocketAddress endpoint) throws IOException {
        throw new IOException("Windows named pipes do not support bind(), pass path to constructor");
    }

    public Socket accept() throws IOException {
        long handle;
        try {
            handle = WindowsPipe.CreateNamedPipe(
                myPath,
                WindowsPipe.PIPE_ACCESS_DUPLEX | WindowsPipe.FILE_FLAG_OVERLAPPED,
                0,
                myMaxInstances,
                BUFFER_SIZE,
                BUFFER_SIZE,
                0,
                WindowsPipe.FILE_ALL_ACCESS);
        } catch (final IOException e) {
            throw new IOException(
                    String.format("Could not create named pipe, error %d", WindowsPipe.GetLastError()));
        }
        myOpenHandles.add(handle);

        long connWaitable = WindowsPipe.CreateEvent(true, false, null);
        long overlapped = WindowsPipe.NewOverlapped(connWaitable);
        try {
            int connectError = WindowsPipe.ConnectNamedPipe(handle, overlapped);
            if (connectError == -1) {
                myOpenHandles.remove(handle);
                myConnectedHandles.add(handle);
                return new WindowsPipeSocket(handle, myCloseCallback);
            }
            if (connectError == WindowsPipe.ERROR_PIPE_CONNECTED) {
                myOpenHandles.remove(handle);
                myConnectedHandles.add(handle);
                return new WindowsPipeSocket(handle, myCloseCallback);
            }
            if (connectError == WindowsPipe.ERROR_NO_DATA) {
                // Client has connected and disconnected between CreateNamedPipe() and ConnectNamedPipe()
                // connection is broken, but it is returned it avoid loop here.
                // Actual error will happen when somebody will try to read/write from/to pipe
                return new WindowsPipeSocket(handle, myCloseCallback);
            }
            if (connectError == WindowsPipe.ERROR_IO_PENDING) {
                if (!WindowsPipe.GetOverlappedResult(handle, overlapped)) {
                    myOpenHandles.remove(handle);
                    WindowsPipe.CloseHandle(handle);
                    throw new IOException("GetOverlappedResult() failed for connect operation, err=" + WindowsPipe.GetLastError());
                }
                myOpenHandles.remove(handle);
                myConnectedHandles.add(handle);
                return new WindowsPipeSocket(handle, myCloseCallback);
            } else
                throw new IOException("ConnectNamedPipe() failed with: " + connectError);
        } finally {
            WindowsPipe.DeleteOverlapped(overlapped);
            WindowsPipe.CloseHandle(connWaitable);
        }
    }

    public void close() throws IOException {
        List<Long> handlesToClose = new ArrayList<>();
        myOpenHandles.drainTo(handlesToClose);
        for (long handle : handlesToClose)
            WindowsPipe.CloseHandle(handle);

        List<Long> handlesToDisconnect = new ArrayList<>();
        myConnectedHandles.drainTo(handlesToDisconnect);
        for (long handle : handlesToDisconnect)
            closeConnectedPipe(handle);
    }

    private void closeConnectedPipe(long handle) {
        WindowsPipe.FlushFileBuffers(handle);
        WindowsPipe.DisconnectNamedPipe(handle);
        WindowsPipe.CloseHandle(handle);
    }
}