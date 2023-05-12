#include "RemoteMessageRouter.h"

RemoteMessageRouter::RemoteMessageRouter(std::shared_ptr<RpcExecutor> service, int id)
    : RemoteServerObjectBase(service, id) {}

RemoteMessageRouter * RemoteMessageRouter::create(std::shared_ptr<RpcExecutor> service) {
  return FACTORY.create([&](int id) -> RemoteMessageRouter* {return new RemoteMessageRouter(service, id);});
}