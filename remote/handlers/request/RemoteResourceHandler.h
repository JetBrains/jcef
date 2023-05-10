#ifndef JCEF_REMOTERESOURCEHANDLER_H
#define JCEF_REMOTERESOURCEHANDLER_H

#include "../RemoteObjectFactory.h"
#include "include/cef_resource_handler.h"

class RemoteResourceHandler : public CefResourceHandler, public RemoteObject<RemoteResourceHandler> {
 public:
  static CefRefPtr<RemoteResourceHandler> create(RemoteClientHandler & owner, thrift_codegen::RObject peer);

  bool Open(CefRefPtr<CefRequest> request,
            bool& handle_request,
            CefRefPtr<CefCallback> callback) override;
  bool ProcessRequest(CefRefPtr<CefRequest> request,
                      CefRefPtr<CefCallback> callback) override;
  void GetResponseHeaders(CefRefPtr<CefResponse> response,
                          int64& response_length,
                          CefString& redirectUrl) override;
  bool Skip(int64 bytes_to_skip,
            int64& bytes_skipped,
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
  explicit RemoteResourceHandler(RemoteClientHandler& owner, int id, int peerId);
  IMPLEMENT_REFCOUNTING(RemoteResourceHandler);
};


#endif  // JCEF_REMOTERESOURCEHANDLER_H
