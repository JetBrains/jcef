#ifndef JCEF_REMOTEMESSAGEROUTER_H
#define JCEF_REMOTEMESSAGEROUTER_H

#include "../RemoteObjectFactory.h"
#include "include/wrapper/cef_message_router.h"

using CefMessageRouter = CefMessageRouterBrowserSide;

class RemoteMessageRouter : public RemoteServerObjectBase<RemoteMessageRouter> {
 public:
  static RemoteMessageRouter * create(std::shared_ptr<RpcExecutor> service);

 private:
  explicit RemoteMessageRouter(std::shared_ptr<RpcExecutor> service, int id);
};

#endif  // JCEF_REMOTEMESSAGEROUTER_H
