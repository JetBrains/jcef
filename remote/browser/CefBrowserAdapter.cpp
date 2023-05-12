#include "CefBrowserAdapter.h"
#include "../log/Log.h"

CefBrowserAdapter::CefBrowserAdapter(CefRefPtr<CefBrowser> browser) : myBrowser(browser) {}

extern void processKeyEvent(
    CefKeyEvent & cef_event,
    int event_type, // event.getID()
    int modifiers,  // event.getModifiersEx()
    char16 key_char, // event.getKeyChar()
    long scanCode,   // event.scancode, windows only
    int key_code   // event.getKeyCode()
);

extern void processMouseEvent(
    CefRefPtr<CefBrowser> browser,
    int event_type, // getID
    int x, // getX
    int y, // getY
    int modifiers, // getModifiersEx
    int click_count, // getClickCount
    int button // getButton
);

extern void processMouseWheelEvent(
    CefRefPtr<CefBrowser> browser,
    int scroll_type, // getScrollType
    int x, // getX
    int y, // getY
    int modifiers, // getModifiersEx
    int delta, // getWheelRotation
    int units_to_scroll // getUnitsToScroll
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
    int event_type = *(p++); // event.getID()
    int x = *(p++);
    int y = *(p++);
    int modifiers = *(p++); // event.getModifiersEx()
    int click_count = *(p++); // getClickCount
    int button = *(p++); // getButton

    processMouseEvent(
        myBrowser,
        event_type,
        x,
        y,
        modifiers,
        click_count,
        button
    );
  }
  else if (method.compare("sendmousewheelevent") == 0) {
    const int len = buffer.size();
    if (len < 4) {
      Log::error("sendmousewheelevent, len %d < 4", len);
      return;
    }

    const int32_t * p = (const int32_t *)buffer.c_str();
    int scroll_type = *(p++);
    int x = *(p++);
    int y = *(p++);
    int modifiers = *(p++);
    int delta = *(p++);
    int units_to_scroll = *(p++);

    processMouseWheelEvent(
        myBrowser,
        scroll_type,
        x,
        y,
        modifiers,
        delta,
        units_to_scroll
    );
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
