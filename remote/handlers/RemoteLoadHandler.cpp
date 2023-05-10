#include "RemoteLoadHandler.h"
#include "../log/Log.h"
#include "RemoteClientHandler.h"

RemoteLoadHandler::RemoteLoadHandler(RemoteClientHandler & owner) : myOwner(owner) {}

void RemoteLoadHandler::OnLoadingStateChange(CefRefPtr<CefBrowser> browser,
                          bool isLoading,
                          bool canGoBack,
                          bool canGoForward) {
  LogNdc ndc("RemoteLoadHandler::OnLoadingStateChange");
  myOwner.exec([&](RpcExecutor::Service s){
    s->onLoadingStateChange(
        myOwner.getBid(),
        isLoading, canGoBack, canGoForward
    );
  });
}

void RemoteLoadHandler::OnLoadStart(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::TransitionType transition_type) {
  LogNdc ndc("RemoteLoadHandler::OnLoadStart");
  myOwner.exec([&](RpcExecutor::Service s){
    s->onLoadStart(myOwner.getBid(), transition_type);
  });
}

void RemoteLoadHandler::OnLoadEnd(CefRefPtr<CefBrowser> browser,
               CefRefPtr<CefFrame> frame,
               int httpStatusCode) {
  LogNdc ndc("RemoteLoadHandler::OnLoadEnd");
  myOwner.exec([&](RpcExecutor::Service s){
    s->onLoadEnd(myOwner.getBid(), httpStatusCode);
  });
}

void RemoteLoadHandler::OnLoadError(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::ErrorCode errorCode,
                 const CefString& errorText,
                 const CefString& failedUrl) {
  LogNdc ndc("RemoteLoadHandler::OnLoadError");
  myOwner.exec([&](RpcExecutor::Service s){
    s->onLoadError(myOwner.getBid(), errorCode, errorText.ToString(), failedUrl.ToString());
  });
}
