package org.cef.callback;

public class CefRunContextMenuCallback_N extends CefNativeAdapter implements CefRunContextMenuCallback {
    @Override
    protected void finalize() throws Throwable {
        cancel();
        super.finalize();
    }

    @Override
    public void Continue(int selected_command_id, int event_flags) {
        try {
            N_Continue(getNativeRef(null), selected_command_id, event_flags);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void cancel() {
        try {
            N_Cancel(getNativeRef(null));
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private native void N_Continue(long self, int selected_command_id, int event_flags);
    private native void N_Cancel(long self);
}
