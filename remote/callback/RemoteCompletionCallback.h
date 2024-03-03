#ifndef JCEF_REMOTECOMPLETIONCALLBACK_H
#define JCEF_REMOTECOMPLETIONCALLBACK_H

#include "../RemoteObjects.h"
#include "include/cef_callback.h"

class RemoteCompletionCallback : public CefCompletionCallback, public RemoteJavaObject<RemoteCompletionCallback> {
 public:
  explicit RemoteCompletionCallback(std::shared_ptr<RpcExecutor> service, thrift_codegen::RObject peer);
  void OnComplete() override;

 private:
  IMPLEMENT_REFCOUNTING(RemoteCompletionCallback);
};


#endif  // JCEF_REMOTECOMPLETIONCALLBACK_H
