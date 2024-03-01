#ifndef JCEF_REMOTEFRAME_H
#define JCEF_REMOTEFRAME_H

#include "../RemoteObjects.h"
#include "../Utils.h"
#include "include/cef_frame.h"

class RemoteFrame : public virtual CefBaseRefCounted, public RemoteServerObjectUpdatable<RemoteFrame, CefFrame> {
 public:
  void updateImpl(const std::map<std::string, std::string>& frameInfo) override {
    // Nothing to do (CefFrame is read-only object).
  }
  std::map<std::string, std::string> toMapImpl() override;

 private:
  explicit RemoteFrame(CefRefPtr<CefFrame> delegate, int id) : RemoteServerObjectUpdatable(id, delegate) {}
  template <class T, class D> friend class ::RemoteServerObjectHolder;
  IMPLEMENT_REFCOUNTING(RemoteFrame);
};

#endif  // JCEF_REMOTEFRAME_H
