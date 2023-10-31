package org.cef.network;

public interface CefPostDataElement {
    /**
     * Post data elements may represent either bytes or files.
     */
    enum Type {
        PDE_TYPE_EMPTY,
        PDE_TYPE_BYTES,
        PDE_TYPE_FILE,
    }

    /**
     * Returns true if this object is read-only.
     */
    boolean isReadOnly();

    /**
     * Remove all contents from the post data element.
     */
    void setToEmpty();

    /**
     * The post data element will represent a file.
     */
    void setToFile(String fileName);

    /**
     * The post data element will represent bytes.  The bytes passed
     * in will be copied.
     */
    void setToBytes(int size, byte[] bytes);

    /**
     * Return the type of this post data element.
     */
    Type getType();

    /**
     * Return the file name.
     */
    String getFile();

    /**
     * Return the number of bytes.
     */
    int getBytesCount();

    /**
     * Read up to size bytes into bytes and return the number of bytes
     * actually read.
     */
    int getBytes(int size, byte[] bytes);

    /**
     * Create a new CefPostDataElement object.
     */
    static CefPostDataElement create() {
        return CefPostDataElement_N.createNative();
    }
}
