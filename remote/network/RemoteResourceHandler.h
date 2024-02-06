#ifndef JCEF_REMOTERESOURCEHANDLER_H
#define JCEF_REMOTERESOURCEHANDLER_H

#include "../RemoteObjects.h"
#include "include/cef_resource_handler.h"

class RemoteResourceHandler : public CefResourceHandler, public RemoteJavaObject<RemoteResourceHandler> {
 public:
  explicit RemoteResourceHandler(int bid, std::shared_ptr<RpcExecutor> service, thrift_codegen::RObject peer);
  ~RemoteResourceHandler();

  bool ProcessRequest(CefRefPtr<CefRequest> request,
                      CefRefPtr<CefCallback> callback) override;
  void GetResponseHeaders(CefRefPtr<CefResponse> response,
                          int64_t& response_length,
                          CefString& redirectUrl) override;
  bool ReadResponse(void* data_out,
                    int bytes_to_read,
                    int& bytes_read,
                    CefRefPtr<CefCallback> callback) override;
  void Cancel() override;

 private:
  const int myBid;
  std::set<int> myCallbacks;
  IMPLEMENT_REFCOUNTING(RemoteResourceHandler);
};


#endif  // JCEF_REMOTERESOURCEHANDLER_H
