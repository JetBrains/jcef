#ifndef JCEF_COMMANDLINEARGS_H
#define JCEF_COMMANDLINEARGS_H

#include <string>
#include <vector>

#include "log/Log.h"

template <class traits> class CefStructBase;
class CefSettingsTraits;
using CefSettings = CefStructBase<CefSettingsTraits>;

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
  int myLogLevel = LEVEL_INFO;
  int myOpenTransportCooldownMs = 3;

  std::vector<std::string> myChromiumSwitches;
  std::vector<std::pair<std::string, int>> myCustomSchemes;
  std::vector<std::pair<std::string, std::string>> myParsedCefSettings;

  friend class ServerApplication;
  void prepareCefSettings(CefSettings & settings);
};

#endif  // JCEF_COMMANDLINEARGS_H
