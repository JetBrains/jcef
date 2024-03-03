#ifndef JCEF_SERVERHANDLERCONTEXT_H
#define JCEF_SERVERHANDLERCONTEXT_H

#include <memory>
#include <string>

class RpcExecutor;
class ClientsManager;
class MessageRoutersManager;

class ServerHandlerContext {
 public:
  ServerHandlerContext();

  const std::shared_ptr<ClientsManager>& clientsManager() { return myClientsManager; }
  const std::shared_ptr<MessageRoutersManager>& routersManager() { return myRoutersManager; }
  const std::shared_ptr<RpcExecutor>& javaService() { return myJavaService; }
  const std::shared_ptr<RpcExecutor>& javaServiceIO() { return myJavaServiceIO; }

  void initJavaServicePipe(const std::string & pipeName);
  void initJavaServicePort(int port);
  void closeJavaServiceTransport();

 private:
  std::shared_ptr<RpcExecutor> myJavaService;
  std::shared_ptr<RpcExecutor> myJavaServiceIO;
  std::shared_ptr<ClientsManager> myClientsManager;
  std::shared_ptr<MessageRoutersManager> myRoutersManager;
};

#endif  // JCEF_SERVERHANDLERCONTEXT_H
