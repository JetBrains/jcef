#include "RemoteCefRunContextMenuCallback.h"

RemoteCefRunContextMenuCallback::RemoteCefRunContextMenuCallback(
    int id,
    CefRefPtr<CefRunContextMenuCallback> delegate)
    : RemoteServerObjectWithCache(id, delegate) {}