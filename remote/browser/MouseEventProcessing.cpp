#include "include/cef_base.h"
#include "include/cef_browser.h"

namespace {
//
// Constants from MouseEvent.java
//
const int MOUSE_FIRST         = 500;
const int MOUSE_LAST          = 507;
const int MOUSE_CLICKED = MOUSE_FIRST;
const int MOUSE_PRESSED = 1 + MOUSE_FIRST;
const int MOUSE_RELEASED = 2 + MOUSE_FIRST;
const int MOUSE_MOVED = 3 + MOUSE_FIRST;
const int MOUSE_ENTERED = 4 + MOUSE_FIRST;
const int MOUSE_EXITED = 5 + MOUSE_FIRST;
const int MOUSE_DRAGGED = 6 + MOUSE_FIRST;
const int MOUSE_WHEEL = 7 + MOUSE_FIRST;
const int NOBUTTON = 0;
const int BUTTON1 = 1;
const int BUTTON2 = 2;
const int BUTTON3 = 3;

// Constants from MouseWheelEvent.java
const int WHEEL_UNIT_SCROLL = 0;
const int WHEEL_BLOCK_SCROLL = 1;
}

extern int GetCefModifiers(int modifiers);

void processMouseEvent(
    CefRefPtr<CefBrowser> browser,
    int event_type, // getID
    int x, // getX
    int y, // getY
    int modifiers, // getModifiersEx
    int click_count, // getClickCount
    int button // getButton
) {
  CefMouseEvent cef_event;
  cef_event.x = x;
  cef_event.y = y;
  cef_event.modifiers = GetCefModifiers(modifiers);

  if (event_type == MOUSE_PRESSED ||
      event_type == MOUSE_RELEASED) {
    CefBrowserHost::MouseButtonType cef_mbt;
    if (button == BUTTON1)
      cef_mbt = MBT_LEFT;
    else if (button == BUTTON2)
      cef_mbt = MBT_MIDDLE;
    else if (button == BUTTON3)
      cef_mbt = MBT_RIGHT;
    else
      return;

    browser->GetHost()->SendMouseClickEvent(
        cef_event, cef_mbt, (event_type == MOUSE_RELEASED),
        click_count);
  } else if (event_type == MOUSE_MOVED ||
             event_type == MOUSE_DRAGGED ||
             event_type == MOUSE_ENTERED ||
             event_type == MOUSE_EXITED) {
    browser->GetHost()->SendMouseMoveEvent(cef_event, (event_type == MOUSE_EXITED));
  }
}

void processMouseWheelEvent(
    CefRefPtr<CefBrowser> browser,
    int scroll_type, // getScrollType
    int x, // getX
    int y, // getY
    int modifiers, // getModifiersEx
    int delta, // getWheelRotation
    int units_to_scroll // getUnitsToScroll
) {
  CefMouseEvent cef_event;
  cef_event.x = x;
  cef_event.y = y;

  cef_event.modifiers = GetCefModifiers(modifiers);

  if (scroll_type == WHEEL_UNIT_SCROLL) {
    // Use the smarter version that considers platform settings.
    delta = units_to_scroll;
  }

  double deltaX = 0, deltaY = 0;
  if (cef_event.modifiers & EVENTFLAG_SHIFT_DOWN)
    deltaX = delta;
  else
#if defined(OS_WIN)
    deltaY = delta * (-1);
#else
    deltaY = delta;
#endif

  browser->GetHost()->SendMouseWheelEvent(cef_event, deltaX, deltaY);
}