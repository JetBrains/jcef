#ifndef JCEF_REMOTEFOCUSHANDLER_H
#define JCEF_REMOTEFOCUSHANDLER_H

#include "include/cef_focus_handler.h"

class RemoteClientHandler;
class RemoteFocusHandler : public CefFocusHandler {
 public:
  explicit RemoteFocusHandler(RemoteClientHandler & owner);
  void OnTakeFocus(CefRefPtr<CefBrowser> browser, bool next) override;
  bool OnSetFocus(CefRefPtr<CefBrowser> browser, FocusSource source) override;
  void OnGotFocus(CefRefPtr<CefBrowser> browser) override;

 private:
  RemoteClientHandler & myOwner;

  IMPLEMENT_REFCOUNTING(RemoteFocusHandler);
};


#endif  // JCEF_REMOTEFOCUSHANDLER_H
