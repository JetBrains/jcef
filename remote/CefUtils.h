#ifndef IPC_CEFUTILS_H
#define IPC_CEFUTILS_H

#include <string>
#include <memory>
#include <vector>
#include <map>
#include <stdexcept>

#include "include/cef_base.h"

class CommandLineArgs;

namespace CefUtils {
  bool initializeCef();
  void runCefLoop();

  bool parseSetting(CefSettings & out, const std::string & settingLine);
  bool parseScheme(std::string & name, int & options, const std::string & settingLine);
}

std::string toString(cef_rect_t& rect);
std::string toString(cef_point_t& pt);

#endif //IPC_CEFUTILS_H
