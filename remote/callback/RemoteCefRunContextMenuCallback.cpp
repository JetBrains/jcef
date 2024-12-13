#include "RemoteCefRunContextMenuCallback.h"
RemoteCefRunContextMenuCallback::RemoteCefRunContextMenuCallback(
    CefRefPtr<CefRunContextMenuCallback> delegate,
    int id)
    : RemoteServerObjectUpdatable(id, delegate) {}