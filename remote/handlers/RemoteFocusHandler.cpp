#include "RemoteFocusHandler.h"
#include "RemoteClientHandler.h"
#include "../Utils.h"
#include "../browser/RemoteBrowser.h"


RemoteFocusHandler::RemoteFocusHandler(std::shared_ptr<RpcExecutor> service) : myService(service) {}

// NOTE: all RemoteFocusHandler methods will be called on the UI thread.

void RemoteFocusHandler::OnTakeFocus(CefRefPtr<CefBrowser> browser, bool next) {
  FIND_BID_OR_RETURN();
  myService->exec([&](const JavaService& s){
    s->FocusHandler_OnTakeFocus(bid, next);
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
  FIND_BID_OR_RETURN_VAL(false);
  return myService->exec<bool>([&](const JavaService& s){
    return s->FocusHandler_OnSetFocus(bid, source2string(source));
  }, false);
}

void RemoteFocusHandler::OnGotFocus(CefRefPtr<CefBrowser> browser) {
  FIND_BID_OR_RETURN();
  myService->exec([&](const JavaService& s){
    s->FocusHandler_OnGotFocus(bid);
  });
}
