#include "RemoteBrowserProcessHandler.h"
#include "../log/Log.h"

RemoteBrowserProcessHandler::RemoteBrowserProcessHandler(std::shared_ptr<BackwardConnection> backwardConnection)
    : ConnectionUser(backwardConnection) {
}

RemoteBrowserProcessHandler::~RemoteBrowserProcessHandler() {}

void RemoteBrowserProcessHandler::OnContextInitialized() {
  LogNdc ndc("RemoteBrowserProcessHandler::OnContextInitialized");
  auto remoteService = getService();
  if (remoteService == nullptr) return;

  try {
    remoteService->onContextInitialized();
  } catch (apache::thrift::TException& tx) {
    onThriftException(tx);
  }
}
