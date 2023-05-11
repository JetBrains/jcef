#include "Log.h"
#include <log4cxx/ndc.h>

#include <cstdarg>
#include <stdexcept>
#include <vector>
#include <thread>

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

  if (NDC::empty())
    NDC::push(""); // for pretty logging
  if (MDC::get("thread.name").empty()) {
    // TODO: pass thread name
    // size_t tidHash = std::hash<std::thread::id>()(std::this_thread::get_id());
    static int tidLocal = 0;
    char buf[64];
    std::sprintf(buf, "th%d", tidLocal++);
    MDC::put("thread.name", buf);
  }

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
      myStartTime(Clock::now()),
      myMsg(msg) {}

void Measurer::append(const std::string & msg) {
  myMsg.append(msg.c_str());
}

Measurer::~Measurer() {
  Duration elapsed = Clock::now() - myStartTime;
  Log::trace("%s | spent %d mcs", myMsg.c_str(), (int)elapsed.count());
}

LogNdc::LogNdc(std::string file, std::string func, std::string threadName) :
      startTime(Clock::now())
{
  std::string msg(file);
  if (!func.empty()) {
    msg.append(":");
    msg.append(func);
  }
  if (!threadName.empty())
    MDC::put("thread.name", threadName);
}

LogNdc::LogNdc(std::string file, std::string func, int thresholdMcs, bool logStart, bool logFinish, std::string threadName) :
      startTime(Clock::now())
{
  std::string msg(file);
  if (!func.empty()) {
    msg.append(":");
    msg.append(func);
  }
  NDC::push(msg);
  if (!threadName.empty())
    MDC::put("thread.name", threadName);
  this->thresholdMcs = thresholdMcs;
  this->logStart = logStart;
  this->logFinish = logFinish;

  if (logStart)
    Log::debug("Start.");
}

LogNdc::~LogNdc() {
  bool logged = false;
  if (thresholdMcs >= 0) {
    Duration elapsed = Clock::now() - startTime;
    const long spentMcs = (long)elapsed.count();
    if (spentMcs >= thresholdMcs) {
      Log::debug("Finished, spent %d msc.", spentMcs);
      logged = true;
    }
  }
  if (!logged && logFinish)
    Log::debug("Finished.");
  NDC::pop();
}
