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
  myService->exec([&](const RpcExecutor::Service& s){
    s->DisplayHandler_OnAddressChange(myBid, frm.get()->serverIdWithMap(), url.ToString());
  });
}

void RemoteDisplayHandler::OnTitleChange(CefRefPtr<CefBrowser> browser,
                   const CefString& title) {
  LNDCT();
  myService->exec([&](const RpcExecutor::Service& s){
    s->DisplayHandler_OnTitleChange(myBid, title.ToString());
  });
}

bool RemoteDisplayHandler::OnTooltip(CefRefPtr<CefBrowser> browser, CefString& text) {
  LNDCT();
  return myService->exec<bool>([&](const RpcExecutor::Service& s){
    return s->DisplayHandler_OnTooltip(myBid, text.ToString());
  }, false);
}

void RemoteDisplayHandler::OnStatusMessage(CefRefPtr<CefBrowser> browser,
                     const CefString& value) {
  LNDCT();
  myService->exec([&](const RpcExecutor::Service& s){
    s->DisplayHandler_OnStatusMessage(myBid, value.ToString());
  });
}

bool RemoteDisplayHandler::OnConsoleMessage(CefRefPtr<CefBrowser> browser,
                      cef_log_severity_t level,
                      const CefString& message,
                      const CefString& source,
                      int line) {
  LNDCT();
  return myService->exec<bool>([&](const RpcExecutor::Service& s){
    return s->DisplayHandler_OnConsoleMessage(myBid, level, message.ToString(), source.ToString(), line);
  }, false);
}
