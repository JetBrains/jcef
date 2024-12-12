//
// Created by Vladimir.Kharitonov on 06/12/2024.
//

#include "RemoteCefRunContextMenuCallback.h"
RemoteCefRunContextMenuCallback::RemoteCefRunContextMenuCallback(
    CefRefPtr<CefRunContextMenuCallback> delegate,
    int id)
    : RemoteServerObject(id, delegate) {}