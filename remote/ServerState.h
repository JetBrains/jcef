#ifndef JCEF_SERVERSTATE_H
#define JCEF_SERVERSTATE_H

#include <string>
#include <mutex>
#include <set>

#include "gen-cpp/Server.h"

class ServerHandler;

class ServerHandlerFactory : virtual public thrift_codegen::ServerIfFactory {
 public:
  ~ServerHandlerFactory() override = default;
  thrift_codegen::ServerIf* getHandler(const ::apache::thrift::TConnectionInfo& connInfo) override;
  void releaseHandler(thrift_codegen::ServerIf* handler) override;

  bool hasMaster();
 private:
  std::recursive_mutex myMutex;
  std::set<const ServerHandler*> myHandlers;
};

class CommandLineArgs {
 public:
  CommandLineArgs();
  void init(int argc, char* argv[]);

  bool useTcp() const { return myUseTcp; }
  int getPort() const { return myPort; }
  std::string getPipe() const { return myPathPipe; }
  std::string getLogFile() const { return myPathLogFile; }
  std::string getParamsFile() const { return myPathParamsFile; }
  bool isTestMode() const { return myIsTestMode; }
  int getLogLevel() const { return myLogLevel; }
  int getOpenTransportCooldownMs() const { return myOpenTransportCooldownMs; }

 private:
  bool myUseTcp = false;
  int myPort = -1;
  std::string myPathPipe;
  std::string myPathLogFile;
  std::string myPathParamsFile;
  bool myIsTestMode = false;
  int myLogLevel = -1;
  int myOpenTransportCooldownMs = 3;
};

class ServerState {
  explicit ServerState();
 public:
  void init(int argc, char* argv[]);
  void startShuttingDown();
  void onServerHandlerClosed(const ServerHandler & handler, std::string remainingBids);
  void onClientDestroyed(std::string remainingBids);

  enum State {
    SS_NEW,
    SS_SHUTTING_DOWN,
    SS_SHUTDOWN
  };
  std::string getStateDesc() const { return myStateDesc; }

  std::shared_ptr<ServerHandlerFactory> getServerHandlerFactory() const { return myFactory; }
  const CommandLineArgs& getCmdArgs() const { return myCmdArgs; }

  static ServerState& instance() { return ourInstance; }
  static void shutdownHard();
 private:
  CommandLineArgs myCmdArgs;
  std::shared_ptr<ServerHandlerFactory> myFactory;
  std::string myStateDesc = "New";
  State myState = SS_NEW;

  void checkShuttingDown(std::string remainingBids);
  static ServerState ourInstance;
};

#endif  // JCEF_SERVERSTATE_H
