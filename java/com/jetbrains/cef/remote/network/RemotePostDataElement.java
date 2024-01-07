package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.thrift_codegen.PostDataElement;
import org.cef.misc.DebugFormatter;
import org.cef.network.CefPostDataElement;

import java.nio.ByteBuffer;

public class RemotePostDataElement extends CefPostDataElement {
    private final PostDataElement myElement;

    public RemotePostDataElement(PostDataElement postDataElement) {
        super();
        myElement = postDataElement;
    }

    @Override
    public void dispose() {}

    public boolean isReadOnly() { return myElement.isReadOnly; }

    @Override
    public void setToEmpty() {
        myElement.file = null;
        myElement.bytes = null;
    }

    @Override
    public void setToFile(String fileName) {
        myElement.file = fileName;
        myElement.bytes = null;
    }

    @Override
    public void setToBytes(int size, byte[] bytes) {
        myElement.file = null;
        myElement.bytes = ByteBuffer.wrap(bytes, 0, size);
    }

    @Override
    public Type getType() {
        if (myElement.file == null && myElement.bytes == null)
            return CefPostDataElement.Type.PDE_TYPE_EMPTY;
        if (myElement.file == null)
            return CefPostDataElement.Type.PDE_TYPE_BYTES;
        return CefPostDataElement.Type.PDE_TYPE_FILE;
    }

    @Override
    public String getFile() { return myElement.file; }

    @Override
    public int getBytesCount() { return myElement.bytes == null ? 0 : myElement.bytes.capacity(); }

    @Override
    public int getBytes(int size, byte[] bytes) {
        if (myElement.bytes == null)
            return 0;

        myElement.bytes.get(bytes, 0, size);
        return size;
    }

    static PostDataElement toThriftWithMap(CefPostDataElement postData) {
        PostDataElement e = new PostDataElement(postData.isReadOnly());
        if (postData.getType() == CefPostDataElement.Type.PDE_TYPE_FILE) {
            e.file = postData.getFile();
        } else if (postData.getType() == CefPostDataElement.Type.PDE_TYPE_BYTES) {
            byte[] buf = new byte[postData.getBytesCount()];
            postData.getBytes(postData.getBytesCount(), buf);
            e.bytes = ByteBuffer.wrap(buf);
        }
        return e;
    }

    @Override
    public String toString() {
        return DebugFormatter.toString_PostDataElement(null, this);
    }
}
