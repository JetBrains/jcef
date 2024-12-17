#ifndef JCEF_REMOTEINTCALLBACK_H
#define JCEF_REMOTEINTCALLBACK_H

#include "../RemoteObjects.h"

class RemoteIntCallback : public virtual CefBaseRefCounted, public RemoteJavaObject<RemoteIntCallback> {
 public:
  explicit RemoteIntCallback(std::shared_ptr<ServerHandlerContext> service, thrift_codegen::RObject peer);
  void OnComplete(int result);

 private:
  IMPLEMENT_REFCOUNTING(RemoteIntCallback);
};

#endif  // JCEF_REMOTEINTCALLBACK_H
