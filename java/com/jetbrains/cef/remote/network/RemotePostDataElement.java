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

    public RemotePostDataElement() {
        super();
        myElement = new PostDataElement(false, type2int(Type.PDE_TYPE_EMPTY));
    }

    @Override
    public void dispose() {}

    public boolean isReadOnly() { return myElement.isReadOnly; }

    @Override
    public void setToEmpty() {
        myElement.file = null;
        myElement.bytes = null;
        myElement.type = type2int(Type.PDE_TYPE_EMPTY);
    }

    @Override
    public void setToFile(String fileName) {
        myElement.file = fileName;
        myElement.bytes = null;
        myElement.type = type2int(Type.PDE_TYPE_FILE);
    }

    @Override
    public void setToBytes(int size, byte[] bytes) {
        myElement.file = null;
        myElement.bytes = ByteBuffer.wrap(bytes, 0, size);
        myElement.type = type2int(Type.PDE_TYPE_BYTES);
    }

    @Override
    public Type getType() { return int2type(myElement.type); }

    @Override
    public String getFile() { return myElement.file; }

    @Override
    public int getBytesCount() { return myElement.bytes == null ? 0 : myElement.bytes.capacity(); }

    @Override
    public int getBytes(int size, byte[] bytes) {
        if (myElement.bytes == null)
            return 0;

        myElement.bytes.position(0);
        myElement.bytes.get(bytes, 0, size);
        return size;
    }

    static PostDataElement toThriftWithMap(CefPostDataElement postData) {
        if (postData == null)
            return null;

        final Type type = postData.getType();
        PostDataElement e = new PostDataElement(postData.isReadOnly(), type2int(type));
        e.file = postData.getFile();
        if (postData.getBytesCount() > 0) {
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

    private static Type int2type(int type) {
//        typedef enum {
//            PDE_TYPE_EMPTY = 0,
//            PDE_TYPE_BYTES,
//            PDE_TYPE_FILE,
//            PDF_TYPE_NUM_VALUES,
//        } cef_postdataelement_type_t;
        switch (type) {
            case 0: return Type.PDE_TYPE_EMPTY;
            case 1: return Type.PDE_TYPE_BYTES;
            case 2: return Type.PDE_TYPE_FILE;
            case 3: return Type.PDE_TYPE_NUM_VALUES;
            default: return null;
        }
    }

    private static int type2int(Type type) {
        switch (type) {
            case PDE_TYPE_EMPTY: return 0;
            case PDE_TYPE_BYTES: return 1;
            case PDE_TYPE_FILE: return 2;
            case PDE_TYPE_NUM_VALUES: return 3;
            default: return -1;
        }
    }
}
