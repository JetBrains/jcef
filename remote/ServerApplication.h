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
class CancellationPoint;

class CommandLineArgs {
 public:
  CommandLineArgs();
  void init(int argc, char* argv[]);

  bool useTcp() const { return myUseTcp; }
  bool waitDebugger() const { return myWaitDebugger; }
  bool deleteRootCacheDir() const { return myDeleteRootCacheDir; }
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
  bool myDeleteRootCacheDir = false;
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

  bool init(int argc, char* argv[]);

  const std::chrono::high_resolution_clock::time_point& getStartTime() const;

  enum State {
    SS_NEW,
    SS_SHUTTING_DOWN,
    SS_SHUTDOWN
  };
  std::string getState();

  std::shared_ptr<apache::thrift::TProcessorFactory> getProcessorFactory() const;

  RemoteAppHandler* getCefAppHandler() { return myAppHandler; }
  const CommandLineArgs& getCmdArgs() const { return myCmdArgs; }
  std::string getRootPath() const;
  bool isDefaultRoot() const;

  void startShuttingDown();
  void onBeforeExit();

  static ServerApplication& instance() { return ourInstance; }

  std::shared_ptr<ServerHandlerContext> getCtx(int connectionId);

 private:
  CommandLineArgs myCmdArgs;
  RemoteAppHandler* myAppHandler = nullptr;
  std::shared_ptr<MyServerProcessorFactory> myFactory;
  State myState = SS_NEW;
  std::recursive_mutex myMutexState;
  std::chrono::high_resolution_clock::time_point myTimeStart;
  std::chrono::high_resolution_clock::time_point myTimeStartShuttingDown;

  std::thread myThreadWatcher;
  std::shared_ptr<CancellationPoint> myStopWatcher;

  std::thread myThreadShutdown;

  static ServerApplication ourInstance;
};

#endif  // JCEF_SERVERAPPLICATION_H
