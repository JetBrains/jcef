package com.jetbrains.cef.remote.network;

import com.jetbrains.cef.remote.thrift_codegen.PostData;
import com.jetbrains.cef.remote.thrift_codegen.PostDataElement;
import org.cef.misc.DebugFormatter;
import org.cef.network.CefPostData;
import org.cef.network.CefPostDataElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class RemotePostData extends CefPostData {
    private final PostData myPostData;
    private final List<CefPostDataElement> myElements = new ArrayList<>();

    public RemotePostData(PostData postData) {
        super();
        myPostData = postData;
        if (myPostData.elements != null)
            myPostData.elements.forEach(e -> {
                myElements.add(new RemotePostDataElement(e));
            });
    }

    @Override
    public void dispose() {}

    public boolean isReadOnly() { return myPostData.isReadOnly; }

    public boolean hasExcludedElements() { return myPostData.hasExcludedElements; }

    @Override
    public int getElementCount() { return myPostData.elements == null ? 0 : myPostData.elements.size(); }

    @Override
    public void getElements(Vector<CefPostDataElement> elements) { elements.addAll(myElements); }

    @Override
    public boolean removeElement(CefPostDataElement element) { return myElements.remove(element); }

    @Override
    public boolean addElement(CefPostDataElement element) { return myElements.add(element); }

    @Override
    public void removeElements() { myElements.clear(); }

    static PostData toThriftWithMap(CefPostData postData) {
        boolean hasExcluded = (postData instanceof RemotePostData) ? ((RemotePostData)postData).hasExcludedElements() : false;
        PostData pd = new PostData(postData.isReadOnly(), hasExcluded);
        if (postData.getElementCount() > 0) {
            Vector<CefPostDataElement> elements = new Vector<>();
            postData.getElements(elements);
            List<PostDataElement> resElements = new ArrayList<>();
            elements.forEach(e -> resElements.add(RemotePostDataElement.toThriftWithMap(e)));
            pd.setElements(resElements);
        }
        return pd;
    }

    @Override
    public String toString() {
        return DebugFormatter.toString_PostData(null, this);
    }
}
