#ifndef JCEF_DEBUGINFO_H
#define JCEF_DEBUGINFO_H
#include <functional>
#include <vector>
#include <string>

class DebugInfo {
public:
  static void addInfoProvider(std::function<std::string()> infoProvider);
  static std::string getInfo(int tabs);
  static std::string getMeasures(int tabs);

  static void addMeasure(const std::string & name, int64_t value);
private:
  static std::vector<std::function<std::string()>> INFO_PROVIDERS;
};

#endif  // JCEF_DEBUGINFO_H
