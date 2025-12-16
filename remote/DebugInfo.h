#ifndef JCEF_DEBUGINFO_H
#define JCEF_DEBUGINFO_H
#include <functional>
#include <vector>
#include <string>

class DebugInfo {
public:
  static void addInfoProvider(std::function<std::string()> infoProvider);
  static std::string getInfo(int tabs);

private:
  static std::vector<std::function<std::string()>> INFO_PROVIDERS;
};

#endif  // JCEF_DEBUGINFO_H
