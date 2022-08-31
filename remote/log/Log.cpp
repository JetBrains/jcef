#include "Log.h"

#include <cstdarg>
#include <stdexcept>
#include <vector>

#include <log4cxx/logger.h>
#include <log4cxx/logmanager.h>
#include <log4cxx/xml/domconfigurator.h>

void Log::init() {
  std::wstring configFile(L"log4cxx.logconfig");
  log4cxx::xml::DOMConfigurator::configure(configFile);

  log4cxx::LoggerPtr logger = log4cxx::Logger::getRootLogger();
  LOG4CXX_INFO((logger),  L"Initialized log4cxx root logger");
}

void Log::log(const LevelPtr & level, const char *const format, ...) {
  log4cxx::LoggerPtr logger = log4cxx::Logger::getRootLogger();
  if (!level->isGreaterOrEqual(logger->getLevel()))
    return;

  auto temp = std::vector<char>{};
  auto length = std::size_t {63};
  std::va_list args;
  while (temp.size() <= length)
  {
    temp.resize(length + 1);
    va_start(args, format);
    const auto status = std::vsnprintf(temp.data(), temp.size(), format, args);
    va_end(args);
    if (status < 0)
      throw std::runtime_error {"string formatting error"};
    length = static_cast<std::size_t>(status);
  }

  std::string msg(temp.data(), length);
  logger->log(level, msg);
}

Measurer::Measurer(const std::string & msg):
      myStartTime(duration_cast<std::chrono::microseconds>(std::chrono::system_clock::now().time_since_epoch())),
      myMsg(msg) {}

void Measurer::append(const std::string & msg) {
  myMsg.append(msg.c_str());
}

Measurer::~Measurer() {
  std::chrono::microseconds endMs = duration_cast<std::chrono::microseconds>(std::chrono::system_clock::now().time_since_epoch());
  Log::trace("%s | spent %d mcs", myMsg.c_str(), endMs.count() - myStartTime.count());
}
