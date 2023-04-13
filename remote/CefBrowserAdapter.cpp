#include "CefBrowserAdapter.h"
#include "log/Log.h"

CefBrowserAdapter::CefBrowserAdapter(CefRefPtr<CefBrowser> browser) : myBrowser(browser) {}

extern void processKeyEvent(
    CefKeyEvent & cef_event,
    int event_type, // event.getID()
    int modifiers,  // event.getModifiersEx()
    char16 key_char, // event.getKeyChar()
    long scanCode,   // event.scancode, windows only
    int key_code   // event.getKeyCode()
);

void CefBrowserAdapter::invoke(const std::string& method, const std::string& buffer) {
  if (method.compare("wasresized") == 0) {
    myBrowser->GetHost()->WasResized();
  }
  else if (method.compare("sendmouseevent") == 0) {
    const int len = buffer.size();
    if (len < 4) {
      Log::error("sendmouseevent, len %d < 4", len);
      return;
    }

    const int32_t * p = (const int32_t *)buffer.c_str();
    int event_type = *(p++);
    int modifiers = *(p++);

    CefMouseEvent cef_event;
    cef_event.x = *(p++);
    cef_event.y = *(p++);

    // TODO: read modifiers and other params
    CefBrowserHost::MouseButtonType cef_mbt = MBT_LEFT;
    myBrowser->GetHost()->SendMouseClickEvent(cef_event, cef_mbt, event_type == 0, 1);
  }
  else if (method.compare("sendmousewheelevent") == 0) {
    const int len = buffer.size();
    if (len < 4) {
      Log::error("sendmousewheelevent, len %d < 4", len);
      return;
    }

    const int32_t * p = (const int32_t *)buffer.c_str();
    int scrollAmount = *(p++);

    CefMouseEvent cef_event;
    cef_event.x = 1;
    cef_event.y = 1;

    // TODO: process other params
    myBrowser->GetHost()->SendMouseWheelEvent(cef_event, scrollAmount, scrollAmount);
  }
  else if (method.compare("sendkeyevent") == 0) {
    const int32_t * p = (const int32_t *)buffer.c_str();
    int event_type = *(p++); // event.getID()
    int modifiers = *(p++); // event.getModifiersEx()
    char16 key_char = *(p++); // event.getKeyChar()
    long scanCode = *(p++);   // event.scancode, windows only
    int key_code = *(p++);  // event.getKeyCode()

    CefKeyEvent cef_event;
    processKeyEvent(cef_event, event_type, modifiers, key_char, scanCode, key_char);
    myBrowser->GetHost()->SendKeyEvent(cef_event);
  }
  else if (method.compare("loadurl") == 0) {
    const char * surl = (const char *)buffer.c_str();
    Log::debug("loadUrl [%d]: %s", myBid, surl);
    myBrowser->GetMainFrame()->LoadURL(surl);
  }
}
