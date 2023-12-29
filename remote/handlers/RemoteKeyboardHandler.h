#ifndef JCEF_REMOTEKEYBOARDHANDLER_H
#define JCEF_REMOTEKEYBOARDHANDLER_H

#include "include/cef_keyboard_handler.h"

class RemoteClientHandler;
class RemoteKeyboardHandler : public CefKeyboardHandler {
 public:
  explicit RemoteKeyboardHandler(RemoteClientHandler & owner);
  bool OnPreKeyEvent(CefRefPtr<CefBrowser> browser,
                     const CefKeyEvent& event,
                     CefEventHandle os_event,
                     bool* is_keyboard_shortcut) override;
  bool OnKeyEvent(CefRefPtr<CefBrowser> browser,
                  const CefKeyEvent& event,
                  CefEventHandle os_event) override;

 private:
  RemoteClientHandler & myOwner;

  IMPLEMENT_REFCOUNTING(RemoteKeyboardHandler);
};


#endif  // JCEF_REMOTEKEYBOARDHANDLER_H
