#ifndef JCEF_CEFSETTINGSPARSER_H
#define JCEF_CEFSETTINGSPARSER_H

#include <string>
#include <vector>
#include "include/cef_base.h"

namespace CefSettingsParser {
  void parseSettings(const std::string & paramsFilePath, std::vector<std::string> & cmdlineSwitches/*output*/, CefSettings & settings/*output*/, std::vector<std::pair<std::string, int>> & schemes/*output*/);
  void fixLoggingSettings(CefSettings & settings/*output*/, int logLevel, const std::string & file);
}

#endif  // JCEF_CEFSETTINGSPARSER_H
