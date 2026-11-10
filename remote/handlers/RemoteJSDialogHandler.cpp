#include "RemoteJSDialogHandler.h"

#include "../RpcExecutor.h"
#include "../Utils.h"
#include "../browser/RemoteBrowser.h"
#include "../callback/RemoteJSDialogCallback.h"

namespace {

std::string jsDialogTypeToString(CefJSDialogHandler::JSDialogType type) {
  switch (type) {
    case JSDIALOGTYPE_ALERT: return "JSDIALOGTYPE_ALERT";
    case JSDIALOGTYPE_CONFIRM: return "JSDIALOGTYPE_CONFIRM";
    case JSDIALOGTYPE_PROMPT: return "JSDIALOGTYPE_PROMPT";
    default: return string_format("JSDIALOGTYPE_%d", static_cast<int>(type));
  }
}

}  // namespace

RemoteJSDialogHandler::RemoteJSDialogHandler(std::shared_ptr<RpcExecutor> service) : myService(service) {}

bool RemoteJSDialogHandler::OnJSDialog(
    CefRefPtr<CefBrowser> browser,
    const CefString& origin_url,
    JSDialogType dialog_type,
    const CefString& message_text,
    const CefString& default_prompt_text,
    CefRefPtr<CefJSDialogCallback> callback,
    bool& suppress_message) {
  suppress_message = false;
  FIND_BID_OR_RETURN_VAL(false);
  const auto jsDialogCallback = RemoteJSDialogCallback::wrapDelegate(callback);
  thrift_codegen::JSDialogResult result;
  result.handled = false;
  result.suppress_message = false;
  myService->exec([&](const JavaService& s) {
    s->JSDialogHandler_OnJSDialog(
        result, bid, origin_url.ToString(), jsDialogTypeToString(dialog_type),
        message_text.ToString(), default_prompt_text.ToString(), jsDialogCallback->toRObject());
  });
  suppress_message = result.suppress_message;
  if (!result.handled)
    RemoteJSDialogCallback::dispose(jsDialogCallback->getId());
  return result.handled;
}

bool RemoteJSDialogHandler::OnBeforeUnloadDialog(
    CefRefPtr<CefBrowser> browser,
    const CefString& message_text,
    bool is_reload,
    CefRefPtr<CefJSDialogCallback> callback) {
  FIND_BID_OR_RETURN_VAL(false);
  const auto jsDialogCallback = RemoteJSDialogCallback::wrapDelegate(callback);
  const bool handled = myService->exec<bool>([&](const JavaService& s) {
    return s->JSDialogHandler_OnBeforeUnloadDialog(
        bid, message_text.ToString(), is_reload, jsDialogCallback->toRObject());
  }, false);
  if (!handled)
    RemoteJSDialogCallback::dispose(jsDialogCallback->getId());
  return handled;
}

void RemoteJSDialogHandler::OnResetDialogState(CefRefPtr<CefBrowser> browser) {
  FIND_BID_OR_RETURN();
  myService->exec([&](const JavaService& s) {
    s->JSDialogHandler_OnResetDialogState(bid);
  });
}

void RemoteJSDialogHandler::OnDialogClosed(CefRefPtr<CefBrowser> browser) {
  FIND_BID_OR_RETURN();
  myService->exec([&](const JavaService& s) {
    s->JSDialogHandler_OnDialogClosed(bid);
  });
}