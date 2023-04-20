#include "RemoteLoadHandler.h"
#include "RemoteClientHandler.h"
#include "log/Log.h"

RemoteLoadHandler::RemoteLoadHandler(RemoteClientHandler & owner) : myOwner(owner) {}

void RemoteLoadHandler::OnLoadingStateChange(CefRefPtr<CefBrowser> browser,
                          bool isLoading,
                          bool canGoBack,
                          bool canGoForward) {
  LogNdc ndc("RemoteLoadHandler::OnLoadingStateChange");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onLoadingStateChange(
        myOwner.getCid(), myOwner.getBid(),
        isLoading, canGoBack, canGoForward
    );
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

void RemoteLoadHandler::OnLoadStart(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::TransitionType transition_type) {
  LogNdc ndc("RemoteLoadHandler::OnLoadStart");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onLoadStart(myOwner.getCid(), myOwner.getBid(), transition_type);
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

void RemoteLoadHandler::OnLoadEnd(CefRefPtr<CefBrowser> browser,
               CefRefPtr<CefFrame> frame,
               int httpStatusCode) {
  LogNdc ndc("RemoteLoadHandler::OnLoadEnd");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onLoadEnd(myOwner.getCid(), myOwner.getBid(), httpStatusCode);
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

void RemoteLoadHandler::OnLoadError(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::ErrorCode errorCode,
                 const CefString& errorText,
                 const CefString& failedUrl) {
  LogNdc ndc("RemoteLoadHandler::OnLoadError");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onLoadError(myOwner.getCid(), myOwner.getBid(), errorCode, errorText.ToString(), failedUrl.ToString());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

