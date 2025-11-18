#include "RemoteLoadHandler.h"
#include "../log/Log.h"
#include "RemoteClientHandler.h"
#include "../browser/RemoteFrame.h"
#include "../browser/RemoteBrowser.h"


RemoteLoadHandler::RemoteLoadHandler(std::shared_ptr<RpcExecutor> service) : myService(service) {}

void RemoteLoadHandler::OnLoadingStateChange(CefRefPtr<CefBrowser> browser,
                          bool isLoading,
                          bool canGoBack,
                          bool canGoForward) {
  FIND_BID_OR_RETURN();
  myService->exec([&](const JavaService& s){
    s->LoadHandler_OnLoadingStateChange(
        bid,
        isLoading, canGoBack, canGoForward
    );
  });
}

void RemoteLoadHandler::OnLoadStart(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::TransitionType transition_type) {
  FIND_BID_OR_RETURN();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const JavaService& s){
    s->LoadHandler_OnLoadStart(bid, frm.serverId(), transition_type);
  });
}

void RemoteLoadHandler::OnLoadEnd(CefRefPtr<CefBrowser> browser,
               CefRefPtr<CefFrame> frame,
               int httpStatusCode) {
  FIND_BID_OR_RETURN();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const JavaService& s){
    s->LoadHandler_OnLoadEnd(bid, frm.serverId(), httpStatusCode);
  });
}

void RemoteLoadHandler::OnLoadError(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::ErrorCode errorCode,
                 const CefString& errorText,
                 const CefString& failedUrl) {
  FIND_BID_OR_RETURN();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const JavaService& s){
    s->LoadHandler_OnLoadError(bid, frm.serverId(), errorCode, errorText.ToString(), failedUrl.ToString());
  });
}
