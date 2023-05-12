package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.thrift_codegen.PostDataElement;
import org.cef.network.CefPostDataElement;
import org.cef.network.CefPostDataElementBase;

import java.nio.ByteBuffer;

public class RemotePostDataElement implements CefPostDataElement {
    private final PostDataElement myElement;

    public RemotePostDataElement(PostDataElement postDataElement) { myElement = postDataElement; }

    @Override
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
    public CefPostDataElementBase.Type getType() {
        if (myElement.file == null && myElement.bytes == null)
            return CefPostDataElementBase.Type.PDE_TYPE_EMPTY;
        if (myElement.file == null)
            return CefPostDataElementBase.Type.PDE_TYPE_BYTES;
        return CefPostDataElementBase.Type.PDE_TYPE_FILE;
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
        if (postData.getType() == CefPostDataElementBase.Type.PDE_TYPE_FILE) {
            e.file = postData.getFile();
        } else if (postData.getType() == CefPostDataElementBase.Type.PDE_TYPE_BYTES) {
            byte[] buf = new byte[postData.getBytesCount()];
            postData.getBytes(postData.getBytesCount(), buf);
            e.bytes = ByteBuffer.wrap(buf);
        }
        return e;
    }

    @Override
    public String toString() {
        return CefPostDataElementBase.toString(null, this);
    }
}
