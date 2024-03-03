#include "ServerHandlerContext.h"

#include "router/MessageRoutersManager.h"
#include "browser/ClientsManager.h"
#include "RpcExecutor.h"

ServerHandlerContext::ServerHandlerContext() :
      myRoutersManager(std::make_shared<MessageRoutersManager>()),
      myClientsManager(std::make_shared<ClientsManager>()) {}

void ServerHandlerContext::initJavaServicePipe(const std::string & pipeName) {
  myJavaService = std::make_shared<RpcExecutor>(pipeName);
  myJavaServiceIO = std::make_shared<RpcExecutor>(pipeName);
}

void ServerHandlerContext::initJavaServicePort(int port) {
  myJavaService = std::make_shared<RpcExecutor>(port);
  myJavaServiceIO = std::make_shared<RpcExecutor>(port);
}

void ServerHandlerContext::closeJavaServiceTransport() {
  if (myJavaService && !myJavaService->isClosed())
    myJavaService->close();
  if (myJavaServiceIO && !myJavaServiceIO->isClosed())
    myJavaServiceIO->close();
}