#include "RemotePermissionHandler.h"
#include "../RpcExecutor.h"
#include "../Utils.h"
#include "../browser/RemoteFrame.h"
#include "../callback/RemoteMediaAccessCallback.h"
#include "../log/Log.h"

const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemotePermissionHandler");

RemotePermissionHandler::RemotePermissionHandler(int bid, std::shared_ptr<RpcExecutor> service) : myBid(bid), myService(service) {}

// Return true and call CefMediaAccessCallback methods either in this method or at a later time to continue or cancel the request.
// Return false to proceed with default handling (immediately).
bool RemotePermissionHandler::OnRequestMediaAccessPermission(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    const CefString& requesting_origin,
    uint32_t requested_permissions,
    CefRefPtr<CefMediaAccessCallback> callback) {
  TRACE();
  RemoteFrame::Holder frm(frame);
  RemoteMediaAccessCallback * mediaAccessCallback = RemoteMediaAccessCallback::wrapDelegate(callback);
  const bool handled = myService->exec<bool>([&](const JavaService& s){
    return s->PermissionHandler_OnRequestMediaAccessPermission(myBid, frm.get()->serverIdWithMap(), requesting_origin.ToString(), requested_permissions, mediaAccessCallback->serverId());
  }, false);
  if (!handled)
    RemoteMediaAccessCallback::dispose(mediaAccessCallback->getId());
  return handled;
}

