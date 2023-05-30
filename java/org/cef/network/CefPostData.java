package org.cef.network;

import java.util.Vector;

/**
 * Class used to represent post data for a web request. The methods of this
 * class may be called on any thread.
 */
public interface CefPostData {
    /**
     * Returns true if this object is read-only.
     */
    boolean isReadOnly();

    /**
     * Returns the number of existing post data elements.
     */
    int getElementCount();

    /**
     * Retrieve the post data elements.
     */
    void getElements(Vector<CefPostDataElement> elements);

    /**
     * Remove the specified post data element. Returns true if the removal
     * succeeds.
     */
    boolean removeElement(CefPostDataElement element);

    /**
     * Add the specified post data element. Returns true if the add succeeds.
     */
    boolean addElement(CefPostDataElement element);

    /**
     * Remove all existing post data elements.
     */
    void removeElements();

    /**
     * Create a new CefPostData object.
     */
    static CefPostData create() {
        return CefPostData_N.createNative();
    }
}
