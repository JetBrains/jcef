#include "RemoteLoadHandler.h"
#include "../log/Log.h"
#include "RemoteClientHandler.h"

RemoteLoadHandler::RemoteLoadHandler(RemoteClientHandler & owner) : myOwner(owner) {}

void RemoteLoadHandler::OnLoadingStateChange(CefRefPtr<CefBrowser> browser,
                          bool isLoading,
                          bool canGoBack,
                          bool canGoForward) {
  LNDCT();
  myOwner.exec([&](RpcExecutor::Service s){
    s->LoadHandler_OnLoadingStateChange(
        myOwner.getBid(),
        isLoading, canGoBack, canGoForward
    );
  });
}

void RemoteLoadHandler::OnLoadStart(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::TransitionType transition_type) {
  LNDCT();
  myOwner.exec([&](RpcExecutor::Service s){
    s->LoadHandler_OnLoadStart(myOwner.getBid(), transition_type);
  });
}

void RemoteLoadHandler::OnLoadEnd(CefRefPtr<CefBrowser> browser,
               CefRefPtr<CefFrame> frame,
               int httpStatusCode) {
  LNDCT();
  myOwner.exec([&](RpcExecutor::Service s){
    s->LoadHandler_OnLoadEnd(myOwner.getBid(), httpStatusCode);
  });
}

void RemoteLoadHandler::OnLoadError(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::ErrorCode errorCode,
                 const CefString& errorText,
                 const CefString& failedUrl) {
  LNDCT();
  myOwner.exec([&](RpcExecutor::Service s){
    s->LoadHandler_OnLoadError(myOwner.getBid(), errorCode, errorText.ToString(), failedUrl.ToString());
  });
}
