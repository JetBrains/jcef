#include "RemoteRequestHandler.h"
#include "../../CefUtils.h"
#include "../../log/Log.h"
#include "../RemoteClientHandler.h"
#include "RemoteAuthCallback.h"
#include "RemoteCallback.h"
#include "RemoteRequest.h"
#include "RemoteResourceRequestHandler.h"

std::string err2str(cef_errorcode_t errorcode);
namespace {
  std::string tstatus2str(cef_termination_status_t status);
}

RemoteRequestHandler::RemoteRequestHandler(RemoteClientHandler & owner) : myOwner(owner) {}

bool RemoteRequestHandler::OnBeforeBrowse(CefRefPtr<CefBrowser> browser,
                    CefRefPtr<CefFrame> frame,
                    CefRefPtr<CefRequest> request,
                    bool user_gesture,
                    bool is_redirect
) {
  LogNdc ndc("RemoteRequestHandler::OnBeforeBrowse");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  RemoteRequest * rr = RemoteRequest::create(myOwner, request);
  Holder<RemoteRequest> holder(*rr);
  try {
    return remoteService->RequestHandler_OnBeforeBrowse(myOwner.getBid(), rr->toThriftWithMap(), user_gesture, is_redirect);
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return false;
}

bool RemoteRequestHandler::OnOpenURLFromTab(CefRefPtr<CefBrowser> browser,
                      CefRefPtr<CefFrame> frame,
                      const CefString& target_url,
                      WindowOpenDisposition target_disposition,
                      bool user_gesture
) {
  LogNdc ndc("RemoteRequestHandler::OnOpenURLFromTab");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  try {
    return remoteService->RequestHandler_OnOpenURLFromTab(myOwner.getBid(), target_url.ToString(), user_gesture);
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return false;
}

CefRefPtr<CefResourceRequestHandler> RemoteRequestHandler::GetResourceRequestHandler(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    bool is_navigation,
    bool is_download,
    const CefString& request_initiator,
    bool& disable_default_handling
) {
  // Called on the browser process IO thread before a resource request is initiated.
  LogNdc ndc("RemoteRequestHandler::GetResourceRequestHandler", "ChromeIO");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return nullptr;

  if (!myResourceRequestHandlerReceived) {
    RemoteRequest * rr = RemoteRequest::create(myOwner, request);
    Holder<RemoteRequest> holder(*rr);
    thrift_codegen::RObject remoteHandler;
    try {
      remoteService->RequestHandler_GetResourceRequestHandler(remoteHandler, myOwner.getBid(), rr->toThriftWithMap(), is_navigation, is_download, request_initiator.ToString());
      myResourceRequestHandlerReceived = true;
      if (!remoteHandler.__isset.isPersistent || !remoteHandler.isPersistent)
        Log::error("Non-persistent ResourceRequestHandler can cause unstable behaviour and won't be used.");
      else if (remoteHandler.objId != -1) {
        myResourceRequestHandler = RemoteResourceRequestHandler::create(myOwner, remoteHandler.objId); // returns ref-ptr wrapper over remote object (disposes java-object in dtor)
        myDisableDefaultHandling = remoteHandler.__isset.isDisableDefaultHandling && remoteHandler.isDisableDefaultHandling;
      }
    } catch (apache::thrift::TException& tx) {
      myOwner.onThriftException(tx);
    }
  }

  disable_default_handling = myDisableDefaultHandling;
  return myResourceRequestHandler;
}

bool RemoteRequestHandler::GetAuthCredentials(CefRefPtr<CefBrowser> browser,
                        const CefString& origin_url,
                        bool isProxy,
                        const CefString& host,
                        int port,
                        const CefString& realm,
                        const CefString& scheme,
                        CefRefPtr<CefAuthCallback> callback
) {
  LogNdc ndc("RemoteRequestHandler::GetAuthCredentials");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  RemoteAuthCallback * rc = RemoteAuthCallback::create(myOwner, callback);
  try {
    return remoteService->RequestHandler_GetAuthCredentials(myOwner.getBid(), origin_url.ToString(), isProxy, host.ToString(), port, realm.ToString(), scheme.ToString(), rc->toThrift());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return false;
}

bool RemoteRequestHandler::OnCertificateError(CefRefPtr<CefBrowser> browser,
                        cef_errorcode_t cert_error,
                        const CefString& request_url,
                        CefRefPtr<CefSSLInfo> ssl_info,
                        CefRefPtr<CefCallback> callback
) {
  LogNdc ndc("RemoteRequestHandler::OnCertificateError");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return false;

  RemoteCallback * rc = RemoteCallback::create(myOwner, callback);
  thrift_codegen::RObject sslInfo;
  sslInfo.objId = -1;
  try {
    return remoteService->RequestHandler_OnCertificateError(myOwner.getBid(), err2str(cert_error), request_url, sslInfo,
        rc->toThriftWithMap());
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
  return false;
}

void RemoteRequestHandler::OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser, TerminationStatus status) {
  LogNdc ndc("RemoteRequestHandler::OnRenderProcessTerminated");
  auto remoteService = myOwner.getService();
  if (remoteService == nullptr) return;

  try {
    return remoteService->RequestHandler_OnRenderProcessTerminated(myOwner.getBid(), tstatus2str(status));
  } catch (apache::thrift::TException& tx) {
    myOwner.onThriftException(tx);
  }
}

namespace {
  std::pair<cef_errorcode_t, std::string> errorcodes [] = {
      #define NET_ERROR(label, value) {ERR_##label,"ERR_"#label},
      #include "include/base/internal/cef_net_error_list.h"
      #undef NET_ERROR
      {ERR_NONE, "ERR_NONE"}
  };

  std::pair<cef_termination_status_t, std::string> terminationStatuses [] = {
      {TS_ABNORMAL_TERMINATION, "TS_ABNORMAL_TERMINATION"},
      {TS_PROCESS_WAS_KILLED, "TS_PROCESS_WAS_KILLED"},
      {TS_PROCESS_CRASHED, "TS_PROCESS_CRASHED"},
      {TS_PROCESS_OOM, "TS_PROCESS_OOM"}
  };

  std::string tstatus2str(cef_termination_status_t status) {
    for (auto p: terminationStatuses) {
      if (p.first == status)
        return p.second;
    }
    return string_format("unknown_termination_status_%d", status);
  }
}

std::string err2str(cef_errorcode_t errorcode) {
    for (auto p: errorcodes) {
      if (p.first == errorcode)
        return p.second;
    }
    Log::error("Can't find cef_errorcode_t: %d", errorcode);
    return string_format("unknown_errorcode_%d", errorcode);
}

cef_errorcode_t str2err(std::string err) {
    for (auto p: errorcodes) {
      if (p.second.compare(err) == 0)
        return p.first;
    }
    Log::error("Can't find cef_errorcode_t: %s", err.c_str());
    return ERR_NONE;
}
