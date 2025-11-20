#ifndef JCEF_REMOTERESPONSE_H
#define JCEF_REMOTERESPONSE_H

#include <map>
#include "../RemoteObjects.h"
#include "../Utils.h"
#include "include/cef_response.h"

class RemoteResponse : public virtual CefBaseRefCounted, public RemoteServerObjectWithCache<RemoteResponse, CefResponse> {
 public:
  explicit RemoteResponse(int id, CefRefPtr<CefResponse> delegate);

  void updateImpl(const std::map<std::string, std::string>& requestInfo) override;
  std::map<std::string, std::string> toMapImpl() override;

 private:

  template <class T, class D> friend class ::RemoteServerObjectHolder;
  IMPLEMENT_REFCOUNTING(RemoteResponse);
};

#endif  // JCEF_REMOTERESPONSE_H
