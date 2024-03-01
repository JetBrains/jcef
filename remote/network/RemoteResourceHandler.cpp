#include "RemoteResourceHandler.h"
#include "../CefUtils.h"
#include "../handlers/RemoteClientHandler.h"
#include "../network/RemoteRequest.h"
#include "../network/RemoteResponse.h"
#include "../callback/RemoteCallback.h"
#include "../log/Log.h"

RemoteResourceHandler::RemoteResourceHandler(
    int bid,
    std::shared_ptr<RpcExecutor> service,
    thrift_codegen::RObject peer)
    : RemoteJavaObject(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->ResourceHandler_Dispose(peer.objId);
          }), myBid(bid) {}

RemoteResourceHandler::~RemoteResourceHandler() {
  // Log::trace("Disposed RemoteResourceHandler %d", myBid);
  // simple protection for leaking via callbacks
  for (auto c: myCallbacks)
    RemoteCallback::dispose(c);
}

///
/// Begin processing the request. To handle the request return true and call
/// CefCallback::Continue() once the response header information is available
/// (CefCallback::Continue() can also be called from inside this method if
/// header information is available immediately). To cancel the request return
/// false.
///
/// WARNING: This method is deprecated. Use Open instead.
///
/*--cef()--*/
bool RemoteResourceHandler::ProcessRequest(CefRefPtr<CefRequest> request,
                                           CefRefPtr<CefCallback> callback) {
  LNDCT();
  RemoteRequest::Holder req(request);
  RemoteCallback * rc = RemoteCallback::wrapDelegate(callback);
  const bool handled = myService->exec<bool>([&](RpcExecutor::Service s){
    return s->ResourceHandler_ProcessRequest(myPeerId, req.get()->serverIdWithMap(), rc->serverId());
  }, false);
  if (!handled)
    RemoteCallback::dispose(rc->getId());
  else
    myCallbacks.insert(rc->getId());
  return handled;
}

///
/// Retrieve response header information. If the response length is not known
/// set |response_length| to -1 and ReadResponse() will be called until it
/// returns false. If the response length is known set |response_length|
/// to a positive value and ReadResponse() will be called until it returns
/// false or the specified number of bytes have been read. Use the |response|
/// object to set the mime type, http status code and other optional header
/// values. To redirect the request to a new URL set |redirectUrl| to the new
/// URL. |redirectUrl| can be either a relative or fully qualified URL.
/// It is also possible to set |response| to a redirect http status code
/// and pass the new URL via a Location header. Likewise with |redirectUrl| it
/// is valid to set a relative or fully qualified URL as the Location header
/// value. If an error occured while setting up the request you can call
/// SetError() on |response| to indicate the error condition.
///
void RemoteResourceHandler::GetResponseHeaders(CefRefPtr<CefResponse> response,
                                               int64_t& response_length,
                                               CefString& redirectUrl) {
  LNDCT();
  RemoteResponse::Holder resp(response);
  thrift_codegen::ResponseHeaders _return;
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceHandler_GetResponseHeaders(_return, myPeerId, resp.get()->serverIdWithMap());
  });
  response_length = _return.length;
  if (_return.__isset.redirectUrl)
    redirectUrl = CefString(_return.redirectUrl);
}

/// Read response data. If data is available immediately copy up to
/// |bytes_to_read| bytes into |data_out|, set |bytes_read| to the number of
/// bytes copied, and return true. To read the data at a later time set
/// |bytes_read| to 0, return true and call CefCallback::Continue() when the
/// data is available. To indicate response completion return false.
bool RemoteResourceHandler::ReadResponse(void* data_out,
                                         int bytes_to_read,
                                         int& bytes_read,
                                         CefRefPtr<CefCallback> callback) {
  RemoteCallback* rc = RemoteCallback::wrapDelegate(callback);
  thrift_codegen::ResponseData _return;
  _return.bytes_read = 0;
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceHandler_ReadResponse(_return, myPeerId, bytes_to_read, rc->serverId());
  });
  if (!_return.continueRead)
    RemoteCallback::dispose(rc->getId());
  else
    myCallbacks.insert(rc->getId());
  bytes_read = _return.bytes_read;
  if (bytes_read > 0)
    memcpy(data_out, _return.data.c_str(), _return.data.size());
  return _return.continueRead;
}

void RemoteResourceHandler::Cancel() {
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceHandler_Cancel(myPeerId);
  });
}
