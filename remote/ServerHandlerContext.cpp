#include "ServerHandlerContext.h"

#include "router/MessageRoutersManager.h"
#include "RpcExecutor.h"

#include <queue>
#include <condition_variable>
#include <utility>

#include "browser/RemoteClient.h"
#include "handlers/RemoteClientHandler.h"

namespace {

template<typename T>
class BlockingQueue {
 public:
  void push(const T& value) {
    std::lock_guard<std::mutex> lock(mutex);
    queue.push(std::move(value));
    condition.notify_one();
  }

  T wait_pop() {  // blocking pop
    std::unique_lock<std::mutex> lock(mutex);
    condition.wait(lock, [this] { return !queue.empty(); });
    if (queue.empty())
      return nullptr;
    T const value = std::move(queue.front());
    queue.pop();
    return value;
  }

  bool empty() {
    std::lock_guard<std::mutex> lock(mutex);
    return queue.empty();
  }

  void notify_one() {
    std::lock_guard<std::mutex> lock(mutex);
    condition.notify_one();
  }

 private:
  std::mutex mutex;
  std::queue<T> queue;
  std::condition_variable condition;
};

void bgExecutorLoop(std::shared_ptr<BackgroundExecutor> exec);

} // namespace

class BackgroundExecutor {
 public:
  BackgroundExecutor() {}

  void setService(std::shared_ptr<RpcExecutor> service, std::shared_ptr<BackgroundExecutor> exec) {
    myService = service;
    myBgThread = std::make_shared<std::thread>(std::bind(&bgExecutorLoop, exec));
    myBgThread->detach();
  }

  // schedules rpc execution (in bg thread)
  void post(JavaVoidRpc rpc) {
    myQueue.push(rpc);
  }

  void run() {
    Log::setThreadName("BackgroundExecutor");
    while (!myStop) {
      JavaVoidRpc rpc = myQueue.wait_pop();
      if (rpc != nullptr)
        myService->exec(rpc);
    }
  }

  void stop() { myStop = true; myQueue.notify_one(); }

 private:
  std::shared_ptr<std::thread> myBgThread;
  BlockingQueue<JavaVoidRpc> myQueue;
  bool myStop = false;
  std::shared_ptr<RpcExecutor> myService; // represents background java thread for 'background' calls execution
};

namespace {
  void bgExecutorLoop(std::shared_ptr<BackgroundExecutor> exec) {
    exec->run();
  }
}

ServerHandlerContext::ServerHandlerContext() :
      myBgExecutor(std::make_shared<BackgroundExecutor>()) {}

void ServerHandlerContext::initJavaServicePipe(const std::string & pipeName) {
  myJavaService = std::make_shared<RpcExecutor>(pipeName);
  myJavaServiceIO = std::make_shared<RpcExecutor>(pipeName);
  myJavaServiceBg = std::make_shared<RpcExecutor>(pipeName);
  myBgExecutor->setService(myJavaServiceBg, myBgExecutor);
}

void ServerHandlerContext::initJavaServicePort(int port) {
  myJavaService = std::make_shared<RpcExecutor>(port);
  myJavaServiceIO = std::make_shared<RpcExecutor>(port);
  myJavaServiceBg = std::make_shared<RpcExecutor>(port);
  myBgExecutor->setService(myJavaServiceBg, myBgExecutor);
}

std::shared_ptr<RemoteClient> ServerHandlerContext::createRemoteClient(int handlersMask, std::shared_ptr<ServerHandlerContext> ctx) {
  const int cid = RemoteClient::genNewCid();
  CefRefPtr<RemoteClientHandler> handler = new RemoteClientHandler(ctx, cid, handlersMask);
  std::shared_ptr<RemoteClient> result = std::make_shared<RemoteClient>(cid, handler);

  std::unique_lock lock(myMutex);
  myClients[cid] = result;
  return result;
}

std::shared_ptr<RemoteClient> ServerHandlerContext::findRemoteClient(int cid) {
  std::unique_lock lock(myMutex);
  const auto & i = myClients.find(cid);
  return i == myClients.end() ? nullptr : i->second;
}

void ServerHandlerContext::disposeRemoteClient(int cid) {
  std::unique_lock lock(myMutex);
  const auto & i = myClients.find(cid);
  if (i == myClients.end())
    return;
  i->second->close();
  myClients.erase(cid);
}

void ServerHandlerContext::invokeLater(JavaVoidRpc rpc) {
  myBgExecutor->post(rpc);
}

void ServerHandlerContext::close() {
  if (myIsClosed)
    return;

  myIsClosed = true;
  std::vector<std::shared_ptr<RemoteClient>> clients;
  {
    std::unique_lock lock(myMutex);
    for (auto kv : myClients)
      if (kv.second)
        clients.push_back(kv.second);
    myClients.clear();
  }

  for (auto c : clients)
    c->close();

  try {
    if (myJavaService && !myJavaService->isClosed())
      myJavaService->close();
    if (myJavaServiceIO && !myJavaServiceIO->isClosed())
      myJavaServiceIO->close();
    if (myJavaServiceBg && !myJavaServiceBg->isClosed())
      myJavaServiceBg->close();
    myBgExecutor->stop();
  } catch (apache::thrift::TException& e) {
    Log::error("Thrift exception in ServerHandlerContext::close: %s", e.what());
  }
}

std::string ServerHandlerContext::getDebugInfo(int tabs, bool detailed) {
  std::stringstream ss;
  for (int i = 0; i < tabs; ++i) ss << "\t";

  if (myIsClosed)
    ss << "Closed ctx " << this << std::endl;
  else {
    ss << "Active ctx " << this << std::endl;

    if (detailed) {
      for (int i = 0; i < tabs; ++i) ss << "\t";
      std::unique_lock lock(myMutex);
      ss << "RemoteClients [count=" << myClients.size() << "]:" << std::endl;
      for (auto i : myClients)
        ss << i.second->getDebugInfo(tabs + 1);
    }
  }

  return ss.str();
}