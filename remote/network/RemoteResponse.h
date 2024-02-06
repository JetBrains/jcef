#ifndef JCEF_REMOTERESPONSE_H
#define JCEF_REMOTERESPONSE_H

#include <map>
#include "../RemoteObjects.h"
#include "../Utils.h"
#include "include/cef_response.h"

class RemoteResponse : public virtual CefBaseRefCounted, public RemoteServerObjectUpdatable<RemoteResponse, CefResponse> {
 public:
  void updateImpl(const std::map<std::string, std::string>& requestInfo) override;
  std::map<std::string, std::string> toMapImpl() override;

 private:
  explicit RemoteResponse(CefRefPtr<CefResponse> delegate, int id)
      : RemoteServerObjectUpdatable<RemoteResponse, CefResponse>(id, delegate) {
  }
  template <class T, class D> friend class ::RemoteServerObjectHolder;
  IMPLEMENT_REFCOUNTING(RemoteResponse);
};

#endif  // JCEF_REMOTERESPONSE_H
