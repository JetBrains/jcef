#ifndef JCEF_REMOTEMESSAGEROUTER_H
#define JCEF_REMOTEMESSAGEROUTER_H

#include "../RemoteObjects.h"
#include "include/wrapper/cef_message_router.h"

using CefMessageRouter = CefMessageRouterBrowserSide;
class ClientsManager;
class RemoteMessageRouterHandler;

// Created by java request (ServerHandler::CreateMessageRouter), disposed by java request
// Stores (and manages lifetime) RemoteMessageRouterHandlers.
//
// TODO: add simple leak protection (link with client's connection)
class RemoteMessageRouter : public RemoteServerObject<RemoteMessageRouter, CefMessageRouter> {
 public:
  static RemoteMessageRouter * create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config);

  const CefMessageRouterConfig& getConfig() const { return myConfig; }

  void AddRemoteHandler(std::shared_ptr<ClientsManager> manager, const thrift_codegen::RObject& handler, bool first);
  void RemoveRemoteHandler(const thrift_codegen::RObject& handler);
  std::shared_ptr<RemoteMessageRouterHandler> FindRemoteHandler(int objId);

 private:
  CefMessageRouterConfig myConfig;
  std::map<int, std::shared_ptr<RemoteMessageRouterHandler>> myHandlers;
  std::recursive_mutex myMutex;

  explicit RemoteMessageRouter(std::shared_ptr<RpcExecutor> service, int id, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config);
};

#endif  // JCEF_REMOTEMESSAGEROUTER_H
