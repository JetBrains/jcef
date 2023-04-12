#include "RemoteLoadHandler.h"
#include "RemoteClientHandler.h"
#include "log/Log.h"

RemoteLoadHandler::RemoteLoadHandler(RemoteClientHandler & owner) : myOwner(owner) {}

void RemoteLoadHandler::OnLoadingStateChange(CefRefPtr<CefBrowser> browser,
                          bool isLoading,
                          bool canGoBack,
                          bool canGoForward) {
  auto remoteService = myOwner.getBackwardConnection()->getHandlersService();
  if (remoteService == nullptr) {
    Log::debug("OnLoadingStateChange, null remote service");
    return;
  }

  try {
    remoteService->onLoadingStateChange(
        myOwner.getCid(), myOwner.getBid(),
        isLoading, canGoBack, canGoForward
    );
  } catch (apache::thrift::TException& tx) {
    _onThriftException(tx);
  }
}

void RemoteLoadHandler::OnLoadStart(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::TransitionType transition_type) {
  auto remoteService = myOwner.getBackwardConnection()->getHandlersService();
  if (remoteService == nullptr) {
    Log::debug("OnLoadStart, null remote service");
    return;
  }

  try {
    remoteService->onLoadStart(myOwner.getCid(), myOwner.getBid(), transition_type);
  } catch (apache::thrift::TException& tx) {
    _onThriftException(tx);
  }
}

void RemoteLoadHandler::OnLoadEnd(CefRefPtr<CefBrowser> browser,
               CefRefPtr<CefFrame> frame,
               int httpStatusCode) {
  auto remoteService = myOwner.getBackwardConnection()->getHandlersService();
  if (remoteService == nullptr) {
    Log::debug("OnLoadEnd, null remote service");
    return;
  }

  try {
    remoteService->onLoadEnd(myOwner.getCid(), myOwner.getBid(), httpStatusCode);
  } catch (apache::thrift::TException& tx) {
    _onThriftException(tx);
  }
}

void RemoteLoadHandler::OnLoadError(CefRefPtr<CefBrowser> browser,
                 CefRefPtr<CefFrame> frame,
                 CefLoadHandler::ErrorCode errorCode,
                 const CefString& errorText,
                 const CefString& failedUrl) {
  auto remoteService = myOwner.getBackwardConnection()->getHandlersService();
  if (remoteService == nullptr) {
    Log::debug("OnLoadError, null remote service");
    return;
  }

  try {
    remoteService->onLoadError(myOwner.getCid(), myOwner.getBid(), errorCode, errorText.ToString(), failedUrl.ToString());
  } catch (apache::thrift::TException& tx) {
    _onThriftException(tx);
  }
}

void RemoteLoadHandler::_onThriftException(apache::thrift::TException e) {
  Log::debug("browser [%d], thrift exception occured: %s", myOwner.getBid(), e.what());
}
