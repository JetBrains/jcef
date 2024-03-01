#include "RemoteLoadHandler.h"
#include "../log/Log.h"
#include "RemoteClientHandler.h"
#include "../browser/RemoteFrame.h"

RemoteLoadHandler::RemoteLoadHandler(int bid, std::shared_ptr<RpcExecutor> service) : myBid(bid), myService(service) {}

void RemoteLoadHandler::OnLoadingStateChange(CefRefPtr<CefBrowser> browser,
                          bool isLoading,
                          bool canGoBack,
                          bool canGoForward) {
  LNDCT();
  myService->exec([&](const RpcExecutor::Service& s){
    s->LoadHandler_OnLoadingStateChange(
        myBid,
        isLoading, canGoBack, canGoForward
    );
  });
}

void RemoteLoadHandler::OnLoadStart(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::TransitionType transition_type) {
  LNDCT();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const RpcExecutor::Service& s){
    s->LoadHandler_OnLoadStart(myBid, frm.get()->serverIdWithMap(), transition_type);
  });
}

void RemoteLoadHandler::OnLoadEnd(CefRefPtr<CefBrowser> browser,
               CefRefPtr<CefFrame> frame,
               int httpStatusCode) {
  LNDCT();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const RpcExecutor::Service& s){
    s->LoadHandler_OnLoadEnd(myBid, frm.get()->serverIdWithMap(), httpStatusCode);
  });
}

void RemoteLoadHandler::OnLoadError(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::ErrorCode errorCode,
                 const CefString& errorText,
                 const CefString& failedUrl) {
  LNDCT();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const RpcExecutor::Service& s){
    s->LoadHandler_OnLoadError(myBid, frm.get()->serverIdWithMap(), errorCode, errorText.ToString(), failedUrl.ToString());
  });
}
