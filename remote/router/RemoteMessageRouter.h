#ifndef JCEF_REMOTEMESSAGEROUTER_H
#define JCEF_REMOTEMESSAGEROUTER_H

#include "../RemoteObjectFactory.h"
#include "include/wrapper/cef_message_router.h"

using CefMessageRouter = CefMessageRouterBrowserSide;
class ClientsManager;

// Created by java request (ServerHandler::CreateMessageRouter), disposed by java request
// TODO: add simple leak protection (link with client's connection)
class RemoteMessageRouter : public RemoteServerObject<RemoteMessageRouter, CefMessageRouter> {
 public:
  static RemoteMessageRouter * create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config);

  const CefMessageRouterConfig& getConfig() const { return myConfig; }

  // TODO: add simple leak protection (save handlers internally and dispose them in dtor)
  void AddRemoteHandler(std::shared_ptr<ClientsManager> manager, const thrift_codegen::RObject& handler, bool first);
  void RemoveRemoteHandler(const thrift_codegen::RObject& handler);

 private:
  CefMessageRouterConfig myConfig;
  explicit RemoteMessageRouter(std::shared_ptr<RpcExecutor> service, int id, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config);
};

#endif  // JCEF_REMOTEMESSAGEROUTER_H
