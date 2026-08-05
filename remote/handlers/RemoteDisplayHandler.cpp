#include "RemoteDisplayHandler.h"
#include "RemoteClientHandler.h"
#include "../browser/RemoteFrame.h"
#include "../browser/RemoteBrowser.h"


RemoteDisplayHandler::RemoteDisplayHandler(std::shared_ptr<RpcExecutor> service)
    : myService(service) {}

void RemoteDisplayHandler::OnAddressChange(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     const CefString& url) {
  FIND_BID_OR_RETURN();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const JavaService& s){
    s->DisplayHandler_OnAddressChange(bid, frm.toRObject(), url.ToString());
  });
}

void RemoteDisplayHandler::OnTitleChange(CefRefPtr<CefBrowser> browser,
                   const CefString& title) {
  FIND_BID_OR_RETURN();
  myService->exec([&](const JavaService& s){
    s->DisplayHandler_OnTitleChange(bid, title.ToString());
  });
}

void RemoteDisplayHandler::OnFullscreenModeChange(CefRefPtr<CefBrowser> browser,
                              bool fullscreen) {
  FIND_BID_OR_RETURN();
  myService->exec([&](const JavaService& s){
    s->DisplayHandler_OnFullscreenModeChange(bid, fullscreen);
  });
}

bool RemoteDisplayHandler::OnTooltip(CefRefPtr<CefBrowser> browser, CefString& text) {
  FIND_BID_OR_RETURN_VAL(false);
  return myService->exec<bool>([&](const JavaService& s){
    return s->DisplayHandler_OnTooltip(bid, text.ToString());
  }, false);
}

void RemoteDisplayHandler::OnStatusMessage(CefRefPtr<CefBrowser> browser,
                     const CefString& value) {
  FIND_BID_OR_RETURN();
  myService->exec([&](const JavaService& s){
    s->DisplayHandler_OnStatusMessage(bid, value.ToString());
  });
}

bool RemoteDisplayHandler::OnConsoleMessage(CefRefPtr<CefBrowser> browser,
                      cef_log_severity_t level,
                      const CefString& message,
                      const CefString& source,
                      int line) {
  FIND_BID_OR_RETURN_VAL(false);
  std::string slevel;
  switch (level) {
    case LOGSEVERITY_VERBOSE: slevel = "verbose"; break;
    case LOGSEVERITY_INFO: slevel = "info"; break;
    case LOGSEVERITY_WARNING: slevel = "warning"; break;
    case LOGSEVERITY_ERROR: slevel = "error"; break;
    case LOGSEVERITY_FATAL: slevel = "fatal"; break;
    case LOGSEVERITY_DISABLE: slevel = "disable"; break;
    case LOGSEVERITY_DEFAULT:
      break;
  }
  return myService->exec<bool>([&](const JavaService& s){
    return s->DisplayHandler_OnConsoleMessage(bid, slevel, message.ToString(), source.ToString(), line);
  }, false);
}
