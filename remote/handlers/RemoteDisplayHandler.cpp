#include "RemoteDisplayHandler.h"
#include "RemoteClientHandler.h"
#include "../browser/RemoteFrame.h"

RemoteDisplayHandler::RemoteDisplayHandler(int bid, std::shared_ptr<RpcExecutor> service)
    : myBid(bid), myService(service) {}

void RemoteDisplayHandler::OnAddressChange(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     const CefString& url) {
  LNDCT();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const JavaService& s){
    s->DisplayHandler_OnAddressChange(myBid, frm.serverId(), url.ToString());
  });
}

void RemoteDisplayHandler::OnTitleChange(CefRefPtr<CefBrowser> browser,
                   const CefString& title) {
  LNDCT();
  myService->exec([&](const JavaService& s){
    s->DisplayHandler_OnTitleChange(myBid, title.ToString());
  });
}

bool RemoteDisplayHandler::OnTooltip(CefRefPtr<CefBrowser> browser, CefString& text) {
  LNDCT();
  return myService->exec<bool>([&](const JavaService& s){
    return s->DisplayHandler_OnTooltip(myBid, text.ToString());
  }, false);
}

void RemoteDisplayHandler::OnStatusMessage(CefRefPtr<CefBrowser> browser,
                     const CefString& value) {
  LNDCT();
  myService->exec([&](const JavaService& s){
    s->DisplayHandler_OnStatusMessage(myBid, value.ToString());
  });
}

bool RemoteDisplayHandler::OnConsoleMessage(CefRefPtr<CefBrowser> browser,
                      cef_log_severity_t level,
                      const CefString& message,
                      const CefString& source,
                      int line) {
  LNDCT();
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
    return s->DisplayHandler_OnConsoleMessage(myBid, slevel, message.ToString(), source.ToString(), line);
  }, false);
}
