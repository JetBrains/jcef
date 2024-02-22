package com.jetbrains.cef.remote;

import org.cef.misc.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class WindowsPipeSocket extends Socket {
    private static final int TRANSPORT_OPEN_COOLDOWN_MS = Utils.getInteger("JCEF_TRANSPORT_OPEN_COOLDOWN_MS", 3);
    private final long myHandle;
    private final Consumer<Long> myCloseCallback;
    private final InputStream myIn;
    private final OutputStream myOut;
    private final long myReaderWait;
    private final long myWriterWait;

    public WindowsPipeSocket(long handle, Consumer<Long> closeCallback) throws IOException {
        myHandle = handle;
        myCloseCallback = closeCallback;
        myReaderWait = WindowsPipe.CreateEvent(true, false, null);
        myWriterWait = WindowsPipe.CreateEvent(true, false, null);
        myIn = new PipeInputStream(handle);
        myOut = new PipeOutputStream(handle);
    }

    public WindowsPipeSocket(String pipeName) throws IOException {
        this(WindowsPipe.OpenFile(WindowsPipe.normalizePipePath(pipeName)), null);
        if (TRANSPORT_OPEN_COOLDOWN_MS > 0) {
            try {
                Thread.sleep(TRANSPORT_OPEN_COOLDOWN_MS);
            } catch (InterruptedException e) {}
        }
    }

    @Override
    public InputStream getInputStream() {
        return myIn;
    }

    @Override
    public OutputStream getOutputStream() {
        return myOut;
    }

    @Override
    public void close() throws IOException {
        if (myCloseCallback != null)
            myCloseCallback.accept(myHandle);
        WindowsPipe.CloseHandle(myHandle);
    }

    @Override
    public void shutdownInput() {}

    @Override
    public void shutdownOutput() {}

    private class PipeInputStream extends InputStream {
        private final long handle;

        PipeInputStream(long handle) {
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
            return WindowsPipe.read(myReaderWait, handle, b, off, len);
        }
    }

    private class PipeOutputStream extends OutputStream {
        private final long handle;

        PipeOutputStream(long handle) {
            this.handle = handle;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] {(byte) (0xFF & b)});
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            WindowsPipe.write(myWriterWait, handle, b, off, len);
        }
    }
}