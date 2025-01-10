#ifndef JCEF_SERVERAPPLICATION_H
#define JCEF_SERVERAPPLICATION_H

#include <string>
#include <mutex>
#include <set>
#include <thread>
#include <chrono>

#include "gen-cpp/Server.h"

class ServerHandler;
class MyServerProcessorFactory;
class ServerHandlerContext;
class RemoteAppHandler;

class CommandLineArgs {
 public:
  CommandLineArgs();
  void init(int argc, char* argv[]);

  bool useTcp() const { return myUseTcp; }
  bool waitDebugger() const { return myWaitDebugger; }
  int getPort() const { return myPort; }
  std::string getPipe() const { return myPathPipe; }
  std::string getTransportDesc() const { return myUseTcp ? "port " + std::to_string(myPort) : "pipe " + myPathPipe; }
  std::string getLogFile() const { return myPathLogFile; }
  std::string getParamsFile() const { return myPathParamsFile; }
  int getLogLevel() const { return myLogLevel; }
  int getOpenTransportCooldownMs() const { return myOpenTransportCooldownMs; }

 private:
  bool myUseTcp = false;
  bool myWaitDebugger = false;
  int myPort = -1;
  std::string myPathPipe;
  std::string myPathLogFile;
  std::string myPathParamsFile;
  int myLogLevel = -1;
  int myOpenTransportCooldownMs = 3;
};

class ServerApplication {
  explicit ServerApplication();
 public:
  ~ServerApplication();

  void init(int argc, char* argv[]);
  void startShuttingDown();
  bool isShuttingDown();

  enum State {
    SS_NEW,
    SS_SHUTTING_DOWN,
    SS_SHUTDOWN
  };
  std::string getStateDesc() const;
  const std::chrono::high_resolution_clock::time_point& getStartTime() const;

  std::shared_ptr<apache::thrift::TProcessorFactory> getProcessorFactory() const;
  void onServerHandlerClosed(const ServerHandler & handler);
  void onClientDestroyed();

  RemoteAppHandler* getCefAppHandler() { return myAppHandler; }
  const CommandLineArgs& getCmdArgs() const { return myCmdArgs; }
  void stopWatcher() { myStopWatcher = true; }

  static ServerApplication& instance() { return ourInstance; }

  std::shared_ptr<ServerHandlerContext> getCtx(int cid);

 private:
  CommandLineArgs myCmdArgs;
  RemoteAppHandler* myAppHandler;
  std::shared_ptr<MyServerProcessorFactory> myFactory;
  std::string myStateDesc = "New";
  std::string myRemainingBrowsersDesc = "";
  State myState = SS_NEW;
  std::recursive_mutex myMutex;
  std::chrono::high_resolution_clock::time_point myStartTime;
  std::chrono::high_resolution_clock::time_point myLastStateChange;
  std::thread myThreadWatcher;
  bool myStopWatcher = false;

  void processShuttingDownIfNecessary();
  void shutdownHard();
  void setState(State state, std::string desc = "");
  static ServerApplication ourInstance;
};

#endif  // JCEF_SERVERAPPLICATION_H
