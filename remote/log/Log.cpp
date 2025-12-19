#include "Log.h"
#include "../Utils.h"

#include <stdio.h>
#include <cstdarg>
#include <stdexcept>
#include <vector>
#include <thread>

#include <boost/date_time/posix_time/posix_time.hpp>

namespace {
  thread_local std::vector<std::string> ourNDC;
  thread_local std::string ourThreadName;
  const std::string ourNdcSeparator = " | ";
  int ourLogLevel = Log::LEVEL_INFO;
  FILE * ourLogFile = nullptr;
  bool ourDoFlush = false;
  bool ourAddNewLine = true;
  bool ourPureMsg = false;
  const std::string ourFinishedMsg = "Finished.";
}

std::string Log::level2str(int serverLogLevel) {
  switch (serverLogLevel) {
    case LEVEL_TRACE: return "trace";
    case LEVEL_DEBUG: return "debug";
    case LEVEL_INFO: return "info";
    case LEVEL_WARN: return "warn";
    case LEVEL_ERROR: return "error";
    case LEVEL_FATAL: return "fatal";
    case LEVEL_DISABLED: return "disabled";
    default: return string_format("unknown_log_level_%d", serverLogLevel);
  }
}

int Log::str2level(std::string serverLogLevel) {
    std::string valLowerCase = serverLogLevel;
    std::transform(valLowerCase.begin(), valLowerCase.end(), valLowerCase.begin(),
                   [](unsigned char in){
                     if (in <= 'Z' && in >= 'A')
                       return in - ('Z' - 'z');
                     return (int)in;
                   });
  if (valLowerCase.find("verb") != valLowerCase.npos || valLowerCase.find("trace") != valLowerCase.npos)
    return LEVEL_TRACE;
  if (valLowerCase.find("debug") != valLowerCase.npos)
    return LEVEL_DEBUG;
  if (valLowerCase.find("info") != valLowerCase.npos)
    return LEVEL_INFO;
  if (valLowerCase.find("warn") != valLowerCase.npos)
    return LEVEL_WARN;
  if (valLowerCase.find("err") != valLowerCase.npos)
    return LEVEL_ERROR;
  if (valLowerCase.find("fatal") != valLowerCase.npos)
    return LEVEL_FATAL;
  if (valLowerCase.find("disable") != valLowerCase.npos)
    return LEVEL_DISABLED;

  // Default val is disabled.
  return LEVEL_DISABLED;
}

std::string Log::cefLogLevel2str(int serverLogLevel) {
  switch (serverLogLevel) {
    case LOGSEVERITY_VERBOSE: return "trace";
    case LOGSEVERITY_INFO: return "info";
    case LOGSEVERITY_WARNING: return "warn";
    case LOGSEVERITY_ERROR: return "error";
    case LOGSEVERITY_FATAL: return "fatal";
    case LOGSEVERITY_DISABLE: return "disabled";
    default: return string_format("unknown_log_level_%d", serverLogLevel);
  }
}

void Log::setThreadName(std::string name) {
  ourThreadName.assign(name);
}

void Log::init(int level, std::string logfile) {
  if (level < 0) level = 0; // max verbose
  if (level >= LEVEL_DISABLED) {
    ourLogLevel = LEVEL_DISABLED;
    return;
  }

  fprintf(stderr, "Initialize cef_server logger: level=%s file='%s'\n", level2str(level).c_str(), logfile.c_str());
  if (!logfile.empty() && logfile.compare("stderr") != 0) {
    FILE* flog = fopen(logfile.c_str(), "a");
    if (flog != nullptr) {
      initImpl(level, flog);
    } else {
      fprintf(stderr,
              "Can't open log file '%s', will be used default (stderr)\n",
              logfile.c_str());
      initImpl(level);
    }
  } else
    initImpl(level);
}

void Log::initImpl(int level, FILE* logFile) {
  ourLogLevel = level;
  if (logFile != nullptr) {
    ourDoFlush = true;
    ourLogFile = logFile;
  } else
    ourLogFile = stderr;
}

bool Log::isDebugEnabled() {
  return ourLogLevel <= LEVEL_DEBUG;
}

bool Log::isTraceEnabled() {
  return ourLogLevel <= LEVEL_TRACE;
}

bool Log::isStdStreamLogger() {
  return ourLogFile == stderr;
}

void Log::log(int level, const char *const format, ...) {
  if (level < ourLogLevel)
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
  std::string ndc;
  if (!ourNDC.empty()) {
    for (auto s: ourNDC) {
      if (!ndc.empty())
        ndc.append(ourNdcSeparator);
      ndc.append(s);
    }
  }

  if (ourThreadName.empty()) {
    // TODO: pass thread name
    // size_t tidHash = std::hash<std::thread::id>()(std::this_thread::get_id());
    static int tidLocal = 0;
    ourThreadName.assign(string_format("th%d", tidLocal++));
  }

  const boost::posix_time::ptime now =  boost::posix_time::microsec_clock::local_time();
  const boost::posix_time::time_duration td = now.time_of_day();
  const long hours        = td.hours();
  const long minutes      = td.minutes();
  const long seconds      = td.seconds();
  const long milliseconds = td.total_milliseconds() - ((hours * 3600 + minutes * 60 + seconds) * 1000);
  char timeBuf[64];
  sprintf(timeBuf, "%02ld:%02ld:%02ld.%03ld", hours, minutes, seconds, milliseconds);

  char * logLevel = "";
  if (level == LEVEL_FATAL) logLevel = "[FATAL]";
  else if (level == LEVEL_ERROR) logLevel = "[ERROR]";
  else if (level == LEVEL_WARN) logLevel = "[WARN]";

  const char * end = ourAddNewLine ? "\n" : "";
  if (ourPureMsg)
    fprintf(ourLogFile, "%s%s%s", logLevel, msg.c_str(), end);
  else if (ndc.empty())
    fprintf(ourLogFile, "%s %s[%s] %s%s", timeBuf, logLevel, ourThreadName.c_str(), msg.c_str(), end);
  else
    fprintf(ourLogFile, "%s %s[%s %s] %s%s", timeBuf, logLevel, ourThreadName.c_str(), ndc.c_str(), msg.c_str(), end);
  if (ourDoFlush)
    fflush(ourLogFile);
}

LogNdc::LogNdc(std::string file, std::string func, std::string threadName) :
      startTime(std::chrono::steady_clock::now())
{
  std::string msg(file);
  if (!func.empty()) {
    msg.append(":");
    msg.append(func);
  }
  if (!threadName.empty())
    ourThreadName.assign(threadName);
}

LogNdc::LogNdc(std::string file, std::string func, int thresholdMcs, bool logStart, bool logFinish, std::string threadName) :
      startTime(std::chrono::steady_clock::now())
{
  std::string msg;
  if (func.empty()) {
    msg.assign(file);
  } else {
    // Make short file name
    for (auto ch: file)
      if (std::isupper(ch))
        msg += ch;

    if (msg.empty())
      msg.assign(file);
    msg.append(":");
    msg.append(func);
  }
  ourNDC.push_back(msg);

  if (!threadName.empty())
    ourThreadName.assign(threadName);

  this->thresholdMcs = thresholdMcs;
  this->logStart = logStart;
  this->logFinish = logFinish;

  if (logStart) {
    Log::debug("Start.");
  }
}

LogNdc::~LogNdc() {
  bool logged = false;
  if (thresholdMcs >= 0) {
    auto elapsedMcs = std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now() - startTime);
    const long spentMcs = (long)elapsedMcs.count();
    if (spentMcs >= thresholdMcs) {
      Log::debug("Finished, spent %d msc.", spentMcs);
      logged = true;
    }
  }
  if (!logged && logFinish) {
    Log::debug(ourFinishedMsg.c_str());
  }

  ourNDC.pop_back();
}
