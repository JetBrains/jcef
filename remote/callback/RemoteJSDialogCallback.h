#ifndef JCEF_REMOTEJSDIALOGCALLBACK_H
#define JCEF_REMOTEJSDIALOGCALLBACK_H

#include "include/cef_jsdialog_handler.h"
#include "../RemoteObjects.h"

class RemoteJSDialogCallback : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteJSDialogCallback, CefJSDialogCallback> {
public:
  explicit RemoteJSDialogCallback(int id, const CefRefPtr<CefJSDialogCallback>& delegate) : RemoteServerObject<RemoteJSDialogCallback, CefJSDialogCallback>(id, delegate) {}

private:
  template <class T, class D> friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteJSDialogCallback);
};

#endif  // JCEF_REMOTEJSDIALOGCALLBACK_H