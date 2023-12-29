#include "RemoteFocusHandler.h"
#include "RemoteClientHandler.h"
#include "../CefUtils.h"

RemoteFocusHandler::RemoteFocusHandler(RemoteClientHandler & owner) : myOwner(owner) {}

// NOTE: all RemoteFocusHandler methods will be called on the UI thread.

void RemoteFocusHandler::OnTakeFocus(CefRefPtr<CefBrowser> browser, bool next) {
  myOwner.exec([&](const RpcExecutor::Service& s){
    s->FocusHandler_OnTakeFocus(myOwner.getBid(), next);
  });
}

namespace {
std::string source2string(CefFocusHandler::FocusSource source) {
  switch (source) {
    case FOCUS_SOURCE_NAVIGATION: return "FOCUS_SOURCE_NAVIGATION";
    case FOCUS_SOURCE_SYSTEM: return "FOCUS_SOURCE_SYSTEM";
    default: return string_format("FOCUS_SOURCE_%d", (int)source);
  }
}
}

bool RemoteFocusHandler::OnSetFocus(CefRefPtr<CefBrowser> browser, FocusSource source) {
  return myOwner.exec<bool>([&](const RpcExecutor::Service& s){
    return s->FocusHandler_OnSetFocus(myOwner.getBid(), source2string(source));
  }, false);
}

void RemoteFocusHandler::OnGotFocus(CefRefPtr<CefBrowser> browser) {
  myOwner.exec([&](const RpcExecutor::Service& s){
    s->FocusHandler_OnGotFocus(myOwner.getBid());
  });
}
