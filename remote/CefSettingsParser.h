#ifndef JCEF_CEFSETTINGSPARSER_H
#define JCEF_CEFSETTINGSPARSER_H

#include <string>
#include <vector>
#include "include/cef_base.h"

namespace CefSettingsParser {
  void parseParamsFile(const std::string & paramsFilePath, std::vector<std::string> & cmdlineSwitches/*output*/, std::vector<std::pair<std::string, std::string>> & parsedSettings/*output*/, std::vector<std::pair<std::string, int>> & schemes/*output*/);
  bool setSettingItem(CefSettings & out, const std::string & name, const std::string & val);

  // Parse from command line
  bool parseCefSchemeWord(const std::string & arg, std::string & name, int & options);
  bool parseCefCmdLineSwitch(const std::string & arg, std::string & out);
  bool parseCefSettingWord(const std::string & arg, std::vector<std::pair<std::string, std::string>> & out);
}

#endif  // JCEF_CEFSETTINGSPARSER_H
