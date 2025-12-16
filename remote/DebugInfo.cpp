#include "DebugInfo.h"

#include <sstream>

std::vector<std::function<std::string()>> DebugInfo::INFO_PROVIDERS;

void DebugInfo::addInfoProvider(std::function<std::string()> f) {
  INFO_PROVIDERS.push_back(f);
}

std::string DebugInfo::getInfo(int tabs) {
  std::stringstream ss;
  for (auto p : INFO_PROVIDERS) {
    for (int i = 0; i < tabs; ++i) ss << "\t";
    ss << p() << std::endl;
  }
  return ss.str();
}
