#ifndef JCEF_REMOTEFILEDIALOGCALLBACK_H
#define JCEF_REMOTEFILEDIALOGCALLBACK_H

#include "include/cef_dialog_handler.h"
#include "../RemoteObjects.h"

class RemoteFileDialogCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteFileDialogCallback, CefFileDialogCallback> {
public:
  explicit RemoteFileDialogCallback(int id, const CefRefPtr<CefFileDialogCallback>& delegate) : RemoteServerObject<RemoteFileDialogCallback, CefFileDialogCallback>(id, delegate) {}

private:
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteFileDialogCallback);
};

#endif  // JCEF_REMOTEFILEDIALOGCALLBACK_H