#ifndef JCEF_REMOTEREQUEST_H
#define JCEF_REMOTEREQUEST_H

#include <map>
#include "../RemoteObjects.h"
#include "../Utils.h"
#include "include/cef_request.h"

class RemoteRequest : public virtual CefBaseRefCounted, public RemoteServerObjectWithCache<RemoteRequest, CefRequest> {
 public:
  explicit RemoteRequest(int id, CefRefPtr<CefRequest> delegate) : RemoteServerObjectWithCache(id, delegate) {}

  void updateImpl(const std::map<std::string, std::string>& requestInfo) override;
  std::map<std::string, std::string> toMapImpl() override;

  static std::shared_ptr<RemoteRequest> create(CefRefPtr<CefRequest> delegate);

 private:
  template <class T, class D> friend class ::RemoteServerObjectHolder;
  IMPLEMENT_REFCOUNTING(RemoteRequest);
};

void fillMap(CefRequest::HeaderMap & out, const std::map<std::string, std::string> & in);
void fillMap(std::map<std::string, std::string> & out, const CefRequest::HeaderMap & in);

#endif  // JCEF_REMOTEREQUEST_H
