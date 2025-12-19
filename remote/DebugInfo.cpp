#include "DebugInfo.h"

#include <map>
#include <sstream>

#include "Utils.h"

namespace {
  struct Measure {
    int64_t count = 0;
    int64_t max;
    int64_t min;
    int64_t sum;
  };
  std::map<std::string, Measure> MEASURES;
  std::mutex MEASURES_MUTEX;

  std::string printMeasure(const std::string & name, const Measure & m, int tabs) {
    std::stringstream ss;
    for (int i = 0; i < tabs; ++i) ss << "\t";
    ss << name << ": count=" << m.count << ", min=" << m.min << ", max=" << m.max << ", avg=" << m.sum/m.count << ", total ms: " << m.sum/1000;
    return ss.str();
  }

  std::string printAllMeasures(int tabs) {
    std::unique_lock lock(MEASURES_MUTEX);
    if (MEASURES.empty())
      return "";

    std::stringstream ss;
    for (auto kv : MEASURES)
      ss << printMeasure(kv.first, kv.second, tabs) << std::endl;

    return ss.str();
  }
}

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

std::string DebugInfo::getMeasures(int tabs) {
  return printAllMeasures(tabs);
}

void DebugInfo::addMeasure(const std::string & name, int64_t value) {
  std::unique_lock lock(MEASURES_MUTEX);
  Measure & m = MEASURES[name];
  if (m.count == 0) {
    m.min = m.max = m.sum = value;
  } else {
    m.max = std::max(m.max, value);
    m.min = std::min(m.min, value);
    m.sum += value;
  }
  m.count++;
}

namespace utils {
  // NOTE: when implementation of this dtor is placed in Utils.cpp then OSX-linker fails.
  Measurer::~Measurer() {
    if (isEnabled)
      DebugInfo::addMeasure(name, std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now() - start).count());
  }
}
