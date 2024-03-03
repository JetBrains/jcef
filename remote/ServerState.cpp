#include "ServerState.h"

#include <thread>

#include "include/base/cef_callback.h"
#include "include/wrapper/cef_closure_task.h"
#include "include/cef_app.h"

#include "log/Log.h"
#include "Utils.h"
#include "ServerHandler.h"
#include "RpcExecutor.h"

bool ServerHandlerFactory::hasMaster() {
  Lock lock(myMutex);
  for (const auto& h: myHandlers)
    if (h->isMaster() && !h->isClosed())
      return true;
  return false;
}

thrift_codegen::ServerIf* ServerHandlerFactory::getHandler(const ::apache::thrift::TConnectionInfo& connInfo) {
  ServerHandler * serverHandler = new ServerHandler;
  Log::trace("Created new ServerHandler: %p", serverHandler);
  const int cooldownMs = ServerState::instance().getCmdArgs().getOpenTransportCooldownMs();
  if (cooldownMs > 0) {
    // system 'cooldown' (otherwise pipe transport may not open on Windows)
    std::this_thread::sleep_for(std::chrono::milliseconds(cooldownMs));
  }
  Lock lock(myMutex);
  myHandlers.insert(serverHandler);
  return serverHandler;
}

void ServerHandlerFactory::releaseHandler(thrift_codegen::ServerIf* handler) {
  Log::trace("Release ServerHandler: %p", handler);
  Lock lock(myMutex);
  myHandlers.erase((const ServerHandler*)handler);
  delete handler;
}

ServerState ServerState::ourInstance;

ServerState::ServerState() : myFactory(std::make_shared<ServerHandlerFactory>()) {}

void ServerState::init(int argc, char* argv[]) {
  myCmdArgs.init(argc, argv);
  Log::init(myCmdArgs.getLogLevel(), myCmdArgs.getLogFile());
}

// Called from ServerHandler::stop
// Thread: ServerHandler-executor
void ServerState::startShuttingDown() {
  myState = SS_SHUTTING_DOWN;
  myStateDesc = "shutting down";
}

void ServerState::checkShuttingDown(std::string remainingBids) {
  if (myState == SS_SHUTTING_DOWN) {
    if (remainingBids.empty()) {
      myState = SS_SHUTDOWN;
      myStateDesc = "quit cef msg loop";
      CefPostTask(TID_UI, base::BindOnce(CefQuitMessageLoop));
      Log::debug("CefQuitMessageLoop will be invoked now (on TID_UI).");
    } else {
      myStateDesc = "shutting down (remaining bids: " + remainingBids + ")";
    }
    Log::debug("Server state: %s", myStateDesc.c_str());
  }
}

void ServerState::onClientDestroyed(std::string remainingBids) {
  checkShuttingDown(remainingBids);
}

void ServerState::onServerHandlerClosed(const ServerHandler & handler, std::string remainingBids) {
  if (handler.isMaster() && !myFactory->hasMaster()) {
    myState = SS_SHUTTING_DOWN;
    myStateDesc = string_format("shutting down (triggered by closing last master handler %p)", &handler);
    Log::debug("ServerHandler %p was closed and there are no master handlers now, so shutting down server.", &handler);
  }

  checkShuttingDown(remainingBids);
}

void ServerState::shutdownHard() {
  Log::info("Start hard shutdown.");
  ourInstance.myState = SS_SHUTDOWN;
  ourInstance.myStateDesc = "quit cef msg loop";
  CefPostTask(TID_UI, base::BindOnce(CefQuitMessageLoop));
  Log::debug("CefQuitMessageLoop is posted (to be executed on UI thread), wait a little before exit...");
  std::this_thread::sleep_for(std::chrono::milliseconds(3000));
  Log::info("Buy!");
  std::exit(0);
}

CommandLineArgs::CommandLineArgs() {
  const char* sval = getenv("JCEF_TRANSPORT_OPEN_COOLDOWN_MS");
  if (sval != nullptr) {
    myOpenTransportCooldownMs = atoi(sval);
    if (myOpenTransportCooldownMs < 0) myOpenTransportCooldownMs = 0;
    if (myOpenTransportCooldownMs > 500) myOpenTransportCooldownMs = 500;
    fprintf(stderr, "\tUse OpenTransportCooldownMs=%d\n", myOpenTransportCooldownMs);
  }
}

void CommandLineArgs::init(int argc, char* argv[]) {
  for (int c = 0; c < argc; ++c) {
    const char * arg = argv[c];
    if (arg == nullptr)
      continue;

    // NOTE: these switches don't conflict with chromium one.
    // See https://peter.sh/experiments/chromium-command-line-switches/
    std::string str(arg);
    size_t tokenPos;
    if ((tokenPos = str.find("--port=")) != str.npos) {
      std::string val = str.substr(tokenPos + 7);
      myPort = std::stoi(val);
      myUseTcp = true;
    } else if ((tokenPos = str.find("--pipe=")) != str.npos) {
      myPathPipe = str.substr(tokenPos + 7);
    } else if ((tokenPos = str.find("--logfile=")) != str.npos) {
      myPathLogFile = str.substr(tokenPos + 10);
    } else if ((tokenPos = str.find("--loglevel=")) != str.npos) {
      std::string sval = str.substr(tokenPos + 11);
      myLogLevel = std::stoi(sval);
      if (myLogLevel < LEVEL_TRACE - 5) myLogLevel = LEVEL_TRACE - 5;
      if (myLogLevel > LEVEL_FATAL) myLogLevel = LEVEL_FATAL;
    } else if ((tokenPos = str.find("--params=")) != str.npos) {
      myPathParamsFile = str.substr(tokenPos + 9);
    } else if (str.find("--testmode") != str.npos) {
      myIsTestMode = true;
    }
  }
}
