#ifndef JCEF_REMOTEFRAME_H
#define JCEF_REMOTEFRAME_H

#include "../RemoteObjects.h"
#include "../Utils.h"
#include "include/cef_frame.h"

class RemoteFrame : public virtual CefBaseRefCounted, public RemoteServerObjectWithCache<RemoteFrame, CefFrame> {
 public:
  explicit RemoteFrame(int id, CefRefPtr<CefFrame> delegate) : RemoteServerObjectWithCache(id, delegate) {}

  void updateImpl(const std::map<std::string, std::string>& frameInfo) override {
    // Nothing to do (CefFrame is read-only object).
  }
  std::map<std::string, std::string> toMapImpl() override;

  static std::shared_ptr<RemoteFrame> create(CefRefPtr<CefFrame> delegate);

 private:
  template <class T, class D> friend class ::RemoteServerObjectHolder;
  IMPLEMENT_REFCOUNTING(RemoteFrame);
};

#endif  // JCEF_REMOTEFRAME_H
