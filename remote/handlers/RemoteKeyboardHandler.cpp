#include "RemoteKeyboardHandler.h"
#include "RemoteClientHandler.h"

#include "../Utils.h"

RemoteKeyboardHandler::RemoteKeyboardHandler(int bid, std::shared_ptr<RpcExecutor> service) : myBid(bid), myService(service) {}

namespace {
  std::string type2str(cef_key_event_type_t type) {
    switch (type) {
      case KEYEVENT_RAWKEYDOWN: return "KEYEVENT_RAWKEYDOWN";
      case KEYEVENT_KEYUP: return "KEYEVENT_KEYUP";
      case KEYEVENT_KEYDOWN: return "KEYEVENT_KEYDOWN";
      case KEYEVENT_CHAR: return "KEYEVENT_CHAR";
    }
    return string_format("Unknown_Keyevent_type_%d", type);
  }

  void fillKeyEvent(thrift_codegen::KeyEvent& out, const CefKeyEvent& event) {
    out.type = type2str(event.type);
    out.modifiers = event.type;
    out.windows_key_code = event.windows_key_code;
    out.native_key_code = event.native_key_code;
    out.is_system_key = event.is_system_key;
    out.character = event.character;
    out.unmodified_character = event.unmodified_character;
    out.focus_on_editable_field = event.focus_on_editable_field;
  }
}

// NOTE: all RemoteKeyboardHandler methods will be called on the UI thread.

bool RemoteKeyboardHandler::OnPreKeyEvent(CefRefPtr<CefBrowser> browser,
                   const CefKeyEvent& event,
                   CefEventHandle os_event,
                   bool* is_keyboard_shortcut) {
  thrift_codegen::KeyEvent keyEvent;
  fillKeyEvent(keyEvent, event);
  return myService->exec<bool>([&](const RpcExecutor::Service& s){
    return s->KeyboardHandler_OnPreKeyEvent(myBid, keyEvent);
  },false);
}

bool RemoteKeyboardHandler::OnKeyEvent(CefRefPtr<CefBrowser> browser,
                const CefKeyEvent& event,
                CefEventHandle os_event) {
  thrift_codegen::KeyEvent keyEvent;
  fillKeyEvent(keyEvent, event);
  return myService->exec<bool>([&](const RpcExecutor::Service& s){
    return s->KeyboardHandler_OnKeyEvent(myBid, keyEvent);
  },false);
}
