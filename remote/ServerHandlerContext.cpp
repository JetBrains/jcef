#include "ServerHandlerContext.h"

#include "router/MessageRoutersManager.h"
#include "browser/ClientsManager.h"
#include "RpcExecutor.h"

#include <queue>
#include <condition_variable>
#include <utility>

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
      myRoutersManager(std::make_shared<MessageRoutersManager>()),
      myClientsManager(std::make_shared<ClientsManager>()),
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

void ServerHandlerContext::closeJavaServiceTransport() {
  Log::trace("Closing java service transport.");
  if (myJavaService && !myJavaService->isClosed())
    myJavaService->close();
  if (myJavaServiceIO && !myJavaServiceIO->isClosed())
    myJavaServiceIO->close();
  if (myJavaServiceBg && !myJavaServiceBg->isClosed())
    myJavaServiceBg->close();
  myBgExecutor->stop();
}

void ServerHandlerContext::invokeLater(JavaVoidRpc rpc) {
  myBgExecutor->post(rpc);
}

void ServerHandlerContext::close() {
  try {
    const bool isEmpty = myClientsManager->closeAllBrowsers();
    // NOTE: if some browser wasn't closed than client won't receive onBeforeClose callback if we close transport here. So do it in destructor.
    if (isEmpty) {
      Log::debug("Closing java service transport because all browsers are closed.");
      closeJavaServiceTransport();
    }
  } catch (apache::thrift::TException& e) {
    Log::error("Thrift exception in ServerHandlerContext::close: %s", e.what());
  }
}