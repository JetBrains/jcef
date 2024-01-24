#ifndef JCEF_REMOTERESPONSE_H
#define JCEF_REMOTERESPONSE_H

#include <map>
#include "../RemoteObjects.h"
#include "../Utils.h"
#include "include/cef_response.h"

class RemoteResponse : public virtual CefBaseRefCounted, public RemoteServerObject<RemoteResponse, CefResponse> {
 public:
  void updateImpl(const std::map<std::string, std::string>& requestInfo) override;
  std::map<std::string, std::string> toMapImpl() override;

  static RemoteResponse * create(CefRefPtr<CefResponse> delegate);

 private:
  explicit RemoteResponse(CefRefPtr<CefResponse> delegate, int id);
  IMPLEMENT_REFCOUNTING(RemoteResponse);
};

#endif  // JCEF_REMOTERESPONSE_H
