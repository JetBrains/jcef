#ifndef REMOTECEFRUNCONTEXTMENUCALLBACK_H
#define REMOTECEFRUNCONTEXTMENUCALLBACK_H

#include <utility>

#include "../RemoteObjects.h"
#include "include/cef_base.h"
#include "include/cef_context_menu_handler.h"

class RemoteCefRunContextMenuCallback final
    : public CefBaseRefCounted, public RemoteServerObjectWithCache<RemoteCefRunContextMenuCallback,
                         CefRunContextMenuCallback> {
public:
  explicit RemoteCefRunContextMenuCallback(int id, CefRefPtr<CefRunContextMenuCallback> delegate);

private:
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteCefRunContextMenuCallback);
};

#endif  // REMOTECEFRUNCONTEXTMENUCALLBACK_H
