#include "RemoteResourceHandler.h"
#include "../CefUtils.h"
#include "../handlers/RemoteClientHandler.h"
#include "../log/Log.h"

RemoteResourceHandler::RemoteResourceHandler(RemoteClientHandler& owner, thrift_codegen::RObject peer)
    : RemoteJavaObject(owner, peer.objId,
        [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) { service->ResourceHandler_Dispose(peer.objId); }) {}

bool RemoteResourceHandler::Open(CefRefPtr<CefRequest> request,
                                 bool& handle_request,
                                 CefRefPtr<CefCallback> callback) {
  // TODO: implement
  Log::error("Unimplemented: RemoteResourceHandler::Open");
  return false;
}

bool RemoteResourceHandler::ProcessRequest(CefRefPtr<CefRequest> request,
                                           CefRefPtr<CefCallback> callback) {
  // TODO: implement
  Log::error("Unimplemented: RemoteResourceHandler::ProcessRequest");
  return false;
}

void RemoteResourceHandler::GetResponseHeaders(CefRefPtr<CefResponse> response,
                                               int64_t& response_length,
                                               CefString& redirectUrl) {
  // TODO: implement
  Log::error("Unimplemented: RemoteResourceHandler::GetResponseHeaders");
}

bool RemoteResourceHandler::Skip(int64_t bytes_to_skip,
                                 int64_t& bytes_skipped,
                                 CefRefPtr<CefResourceSkipCallback> callback) {
  // TODO: implement
  Log::error("Unimplemented: RemoteResourceHandler::Skip");
  return false;
}

bool RemoteResourceHandler::Read(void* data_out,
                                 int bytes_to_read,
                                 int& bytes_read,
                                 CefRefPtr<CefResourceReadCallback> callback) {
  // TODO: implement
  Log::error("Unimplemented: RemoteResourceHandler::Read");
  return false;
}

bool RemoteResourceHandler::ReadResponse(void* data_out,
                                         int bytes_to_read,
                                         int& bytes_read,
                                         CefRefPtr<CefCallback> callback) {
  // TODO: implement
  Log::error("Unimplemented: RemoteResourceHandler::ReadResponse");
  return false;
}

void RemoteResourceHandler::Cancel() {
  // TODO: implement
  Log::error("Unimplemented: RemoteResourceHandler::Cancel");
}
