package org.cef.callback;

public interface CefRunContextMenuCallback {
    /**
     *  Called to allow custom display of the context menu. |params| provides
     *  information about the context menu state. |model| contains the context
     *  menu model resulting from OnBeforeContextMenu. For custom display return
     *  true and execute |callback| either synchronously or asynchronously with
     *  the selected command ID. For default display return false. Do not keep
     *  references to |params| or |model| outside of this callback.
     */
    void Continue(int selected_command_id, int event_flags);

    /**
     * Cancel quick menu display.
     */
    void cancel();
}
