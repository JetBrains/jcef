#ifndef JCEF_REMOTERESOURCEHANDLER_H
#define JCEF_REMOTERESOURCEHANDLER_H

#include "../RemoteObjects.h"
#include "include/cef_resource_handler.h"

class RemoteResourceHandler : public CefResourceHandler, public RemoteJavaObject<RemoteResourceHandler> {
 public:
  explicit RemoteResourceHandler(RemoteClientHandler& owner, thrift_codegen::RObject peer);

  bool Open(CefRefPtr<CefRequest> request,
            bool& handle_request,
            CefRefPtr<CefCallback> callback) override;
  bool ProcessRequest(CefRefPtr<CefRequest> request,
                      CefRefPtr<CefCallback> callback) override;
  void GetResponseHeaders(CefRefPtr<CefResponse> response,
                          int64_t& response_length,
                          CefString& redirectUrl) override;
  bool Skip(int64_t bytes_to_skip,
            int64_t& bytes_skipped,
            CefRefPtr<CefResourceSkipCallback> callback) override;
  bool Read(void* data_out,
            int bytes_to_read,
            int& bytes_read,
            CefRefPtr<CefResourceReadCallback> callback) override;
  bool ReadResponse(void* data_out,
                    int bytes_to_read,
                    int& bytes_read,
                    CefRefPtr<CefCallback> callback) override;
  void Cancel() override;

 private:
  IMPLEMENT_REFCOUNTING(RemoteResourceHandler);
};


#endif  // JCEF_REMOTERESOURCEHANDLER_H
