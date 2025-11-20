#ifndef JCEF_REMOTEMESSAGEROUTER_H
#define JCEF_REMOTEMESSAGEROUTER_H

#include "../RemoteObjects.h"
#include "include/wrapper/cef_message_router.h"

using CefMessageRouter = CefMessageRouterBrowserSide;
class RemoteMessageRouterHandler;

// Created by java request (ServerHandler::CreateMessageRouter), disposed by java request.
// Owned by MessageRoutersManage (it's lifetime: per connection)
// Stores (and manages lifetime) RemoteMessageRouterHandlers.
class RemoteMessageRouter : public RemoteServerObject<RemoteMessageRouter, CefMessageRouter> {
 public:
  static RemoteMessageRouter * create(std::shared_ptr<ServerHandlerContext> ctx, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config);
  static RemoteMessageRouter * create(std::shared_ptr<ServerHandlerContext> ctx, const std::string& query, const std::string& cancel);

  const CefMessageRouterConfig& getConfig() const { return myConfig; }

  void AddRemoteHandler(const thrift_codegen::RObject& handler, bool first);
  void RemoveRemoteHandler(const thrift_codegen::RObject& handler);
  std::shared_ptr<RemoteMessageRouterHandler> FindRemoteHandler(int objId);

 private:
  std::shared_ptr<ServerHandlerContext> myCtx;

  CefMessageRouterConfig myConfig;
  std::map<int, std::shared_ptr<RemoteMessageRouterHandler>> myHandlers;
  std::recursive_mutex myMutex;

  explicit RemoteMessageRouter(std::shared_ptr<ServerHandlerContext> ctx, int id, CefRefPtr<CefMessageRouter> delegate, CefMessageRouterConfig config);
};

#endif  // JCEF_REMOTEMESSAGEROUTER_H
