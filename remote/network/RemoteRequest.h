#ifndef JCEF_REMOTEREQUEST_H
#define JCEF_REMOTEREQUEST_H

#include <map>
#include "../RemoteObjects.h"
#include "../Utils.h"
#include "include/cef_request.h"

class RemoteRequest : public virtual CefBaseRefCounted, public RemoteServerObjectUpdatable<RemoteRequest, CefRequest> {
 public:
  void updateImpl(const std::map<std::string, std::string>& requestInfo) override;
  std::map<std::string, std::string> toMapImpl() override;

 private:
  explicit RemoteRequest(CefRefPtr<CefRequest> delegate, int id) : RemoteServerObjectUpdatable(id, delegate) {}
  template <class T, class D> friend class ::RemoteServerObjectHolder;
  IMPLEMENT_REFCOUNTING(RemoteRequest);
};

void fillMap(CefRequest::HeaderMap & out, const std::map<std::string, std::string> & in);
void fillMap(std::map<std::string, std::string> & out, const CefRequest::HeaderMap & in);

#endif  // JCEF_REMOTEREQUEST_H
