#include "RemotePermissionHandler.h"
#include "../RpcExecutor.h"
#include "../Utils.h"
#include "../browser/RemoteFrame.h"
#include "../callback/RemoteMediaAccessCallback.h"
#include "../log/Log.h"
#include "../browser/RemoteBrowser.h"


const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemotePermissionHandler");

RemotePermissionHandler::RemotePermissionHandler(std::shared_ptr<RpcExecutor> service) : myService(service) {}

// Return true and call CefMediaAccessCallback methods either in this method or at a later time to continue or cancel the request.
// Return false to proceed with default handling (immediately).
bool RemotePermissionHandler::OnRequestMediaAccessPermission(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    const CefString& requesting_origin,
    uint32_t requested_permissions,
    CefRefPtr<CefMediaAccessCallback> callback) {
  FIND_BID_OR_RETURN_VAL(false);
  RemoteFrame::Holder frm(frame);
  std::shared_ptr<RemoteMediaAccessCallback> mediaAccessCallback = RemoteMediaAccessCallback::wrapDelegate(callback);
  const bool handled = myService->exec<bool>([&](const JavaService& s){
    return s->PermissionHandler_OnRequestMediaAccessPermission(bid, frm.toRObject(), requesting_origin.ToString(), requested_permissions, mediaAccessCallback->toRObject());
  }, false);
  if (!handled)
    RemoteMediaAccessCallback::dispose(mediaAccessCallback->getId());
  return handled;
}

