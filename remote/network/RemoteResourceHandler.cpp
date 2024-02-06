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
  Log::trace("Disposed RemoteResourceHandler %d", myBid);
  // simple protection for leaking via callbacks
  for (auto c: myCallbacks)
    RemoteCallback::dispose(c);
}

bool RemoteResourceHandler::ProcessRequest(CefRefPtr<CefRequest> request,
                                           CefRefPtr<CefCallback> callback) {
  LNDCT();
  RemoteRequest * rr = RemoteRequest::create(request);
  Holder<RemoteRequest> holder(*rr);
  thrift_codegen::RObject rc = RemoteCallback::create(callback);
  const bool handled = myService->exec<bool>([&](RpcExecutor::Service s){
    return s->ResourceHandler_ProcessRequest(myPeerId, rr->serverId(), rc);
  }, false);
  if (!handled)
    RemoteCallback::dispose(rc.objId);
  else
    myCallbacks.insert(rc.objId);
  return handled;
}

/**
     * Retrieve response header information. If the response length is not known set
     * |responseLength| to -1 and readResponse() will be called until it returns false. If the
     * response length is known set |responseLength| to a positive value and readResponse() will be
     * called until it returns false or the specified number of bytes have been read. Use the
     * |response| object to set the mime type, http status code and other optional header values.
     * @param response The request response that should be returned. Instance only valid within the
     *         scope of this method.
     * @param responseLength Optionally set the response length if known.
     * @param redirectUrl Optionally redirect the request to a new URL.
 */
void RemoteResourceHandler::GetResponseHeaders(CefRefPtr<CefResponse> response,
                                               int64_t& response_length,
                                               CefString& redirectUrl) {
  LNDCT();
  RemoteResponse * rr = RemoteResponse::create(response);
  Holder<RemoteResponse> holder(*rr);
  thrift_codegen::ResponseHeaders _return;
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceHandler_GetResponseHeaders(_return, myPeerId, rr->serverId());
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
  thrift_codegen::RObject rc = RemoteCallback::create(callback);
  thrift_codegen::ResponseData _return;
  _return.bytes_read = 0;
  myService->exec([&](RpcExecutor::Service s){
    s->ResourceHandler_ReadResponse(_return, myPeerId, bytes_to_read, rc);
  });
  if (!_return.continueRead)
    RemoteCallback::dispose(rc.objId);
  else
    myCallbacks.insert(rc.objId);
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
