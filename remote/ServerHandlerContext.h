#ifndef JCEF_SERVERHANDLERCONTEXT_H
#define JCEF_SERVERHANDLERCONTEXT_H

#include <memory>
#include <string>
#include <thread>
#include <queue>
#include <functional>
#include <map>
#include <mutex>

class RemoteClient;
class RpcExecutor;
class ClientsManager;
class MessageRoutersManager;
class BackgroundExecutor;

namespace thrift_codegen { class ClientHandlersClient; }
typedef std::function<void(std::shared_ptr<thrift_codegen::ClientHandlersClient>)> JavaVoidRpc;

class ServerHandlerContext {
 public:
  ServerHandlerContext();

  const std::shared_ptr<RpcExecutor>& javaService() { return myJavaService; }
  const std::shared_ptr<RpcExecutor>& javaServiceIO() { return myJavaServiceIO; }

  void initJavaServicePipe(const std::string & pipeName);
  void initJavaServicePort(int port);

  std::shared_ptr<RemoteClient> createRemoteClient(int handlersMask, std::shared_ptr<ServerHandlerContext> ctx);
  std::shared_ptr<RemoteClient> findRemoteClient(int cid);
  void disposeRemoteClient(int cid);

  // schedules rpc execution (in bg thread)
  void invokeLater(JavaVoidRpc rpc);

  void close();
 private:
  std::shared_ptr<RpcExecutor> myJavaService;
  std::shared_ptr<RpcExecutor> myJavaServiceIO;
  std::shared_ptr<RpcExecutor> myJavaServiceBg; // represents background java thread for 'background' calls execution
  std::shared_ptr<BackgroundExecutor> myBgExecutor;
  bool myIsClosed = false;

  std::mutex myMutex;
  std::map<int, std::shared_ptr<RemoteClient>> myClients;
};

#endif  // JCEF_SERVERHANDLERCONTEXT_H
