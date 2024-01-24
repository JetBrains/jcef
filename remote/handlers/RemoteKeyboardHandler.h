#ifndef JCEF_REMOTEKEYBOARDHANDLER_H
#define JCEF_REMOTEKEYBOARDHANDLER_H

#include "include/cef_keyboard_handler.h"

class RemoteClientHandler;
class RpcExecutor;

class RemoteKeyboardHandler : public CefKeyboardHandler {
 public:
  explicit RemoteKeyboardHandler(int bid, std::shared_ptr<RpcExecutor> service);
  bool OnPreKeyEvent(CefRefPtr<CefBrowser> browser,
                     const CefKeyEvent& event,
                     CefEventHandle os_event,
                     bool* is_keyboard_shortcut) override;
  bool OnKeyEvent(CefRefPtr<CefBrowser> browser,
                  const CefKeyEvent& event,
                  CefEventHandle os_event) override;

 private:
  const int myBid;
  std::shared_ptr<RpcExecutor> myService;

  IMPLEMENT_REFCOUNTING(RemoteKeyboardHandler);
};


#endif  // JCEF_REMOTEKEYBOARDHANDLER_H
