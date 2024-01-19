package com.jetbrains.cef.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Win32NamedPipeSocket extends Socket {
    private static long createFile(String pipeName) throws IOException {
        return Win32Pipe.CreateFile(pipeName);
    }

    private static CloseCallback emptyCallback() {
        return new CloseCallback() {
            public void onNamedPipeSocketClose(long handle) throws IOException {}
        };
    }

    static final boolean DEFAULT_REQUIRE_STRICT_LENGTH = false;
    private final long handle;
    private final CloseCallback closeCallback;
    private final boolean requireStrictLength;
    private final InputStream is;
    private final OutputStream os;
    private final long readerWaitable;
    private final long writerWaitable;

    interface CloseCallback {
        void onNamedPipeSocketClose(long handle) throws IOException;
    }

    /**
     * The doc for InputStream#read(byte[] b, int off, int len) states that "An attempt is made to
     * read as many as len bytes, but a smaller number may be read." However, using
     * requireStrictLength, NGWin32NamedPipeSocketInputStream can require that len matches up exactly
     * the number of bytes to read.
     */
    public Win32NamedPipeSocket(
            long handle, CloseCallback closeCallback, boolean requireStrictLength)
            throws IOException {
        this.handle = handle;
        this.closeCallback = closeCallback;
        this.requireStrictLength = requireStrictLength;
        this.readerWaitable = Win32Pipe.CreateEvent(true, false, null);
        writerWaitable = Win32Pipe.CreateEvent(true, false, null);
        this.is = new Win32NamedPipeSocketInputStream(handle);
        this.os = new Win32NamedPipeSocketOutputStream(handle);
    }

    Win32NamedPipeSocket(long handle, CloseCallback closeCallback) throws IOException {
        this(handle, closeCallback, DEFAULT_REQUIRE_STRICT_LENGTH);
    }

    public Win32NamedPipeSocket(String pipeName) throws IOException {
        this(createFile(pipeName), emptyCallback(), DEFAULT_REQUIRE_STRICT_LENGTH);
    }

    @Override
    public InputStream getInputStream() {
        return is;
    }

    @Override
    public OutputStream getOutputStream() {
        return os;
    }

    @Override
    public void close() throws IOException {
        closeCallback.onNamedPipeSocketClose(handle);
        Win32Pipe.CloseHandle(handle);
    }

    @Override
    public void shutdownInput() throws IOException {}

    @Override
    public void shutdownOutput() throws IOException {}

    private class Win32NamedPipeSocketInputStream extends InputStream {
        private final long handle;

        Win32NamedPipeSocketInputStream(long handle) {
            this.handle = handle;
        }

        @Override
        public int read() throws IOException {
            int result;
            byte[] b = new byte[1];
            if (read(b) == 0) {
                result = -1;
            } else {
                result = 0xFF & b[0];
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return Win32Pipe.read(readerWaitable, handle, b, off, len, requireStrictLength);
        }
    }

    private class Win32NamedPipeSocketOutputStream extends OutputStream {
        private final long handle;

        Win32NamedPipeSocketOutputStream(long handle) {
            this.handle = handle;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] {(byte) (0xFF & b)});
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            Win32Pipe.write(writerWaitable, handle, b, off, len);
        }
    }
}