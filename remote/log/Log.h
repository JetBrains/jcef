#ifndef JCEF_LOG_H
#define JCEF_LOG_H

#include <chrono>
#include <string>

#include "include/cef_base.h"

const int LEVEL_DISABLED = 100;
const int LEVEL_FATAL = 10;
const int LEVEL_ERROR = 9;
const int LEVEL_WARN = 8;
const int LEVEL_INFO = 7;
const int LEVEL_DEBUG = 6;
const int LEVEL_TRACE = 5;

class Log {
public:
  static void init(int level, std::string logfile);
  static bool isDebugEnabled();
  static bool isTraceEnabled();
  
  template <class ... Args>
  static void fatal(const char *const format, Args ... args) {
    log(LEVEL_FATAL, format, args...);
  }
  template <class ... Args>
  static void error(const char *const format, Args ... args) {
    log(LEVEL_ERROR, format, args...);
  }
  template <class ... Args>
  static void warn(const char *const format, Args ... args) {
    log(LEVEL_WARN, format, args...);
  }
  template <class ... Args>
  static void info(const char *const format, Args ... args) {
    log(LEVEL_INFO, format, args...);
  }
  template <class ... Args>
  static void debug(const char *const format, Args ... args) {
    log(LEVEL_DEBUG, format, args...);
  }
  template <class ... Args>
  static void trace(const char *const format, Args ... args) {
    log(LEVEL_TRACE, format, args...);
  }

  static void log(int level, const char *const format, ...);

  static bool isEqual(int serverLogLevel, cef_log_severity_t cefLogLevel) {
    switch (cefLogLevel) {
      case LOGSEVERITY_DISABLE: return serverLogLevel >= LEVEL_DISABLED;
      case LOGSEVERITY_FATAL: return serverLogLevel == LEVEL_FATAL;
      case LOGSEVERITY_ERROR: return serverLogLevel == LEVEL_ERROR;
      case LOGSEVERITY_WARNING: return serverLogLevel == LEVEL_WARN;
      case LOGSEVERITY_INFO: return serverLogLevel == LEVEL_INFO;
      case LOGSEVERITY_DEBUG: return serverLogLevel == LEVEL_DEBUG || serverLogLevel == LEVEL_TRACE;
      case LOGSEVERITY_DEFAULT: return serverLogLevel == LEVEL_INFO;
    }
    return false;
  }
  static cef_log_severity_t toCefLogLevel(int serverLogLevel) {
    switch (serverLogLevel) {
      case LEVEL_DISABLED: return LOGSEVERITY_DISABLE;
      case LEVEL_FATAL: return LOGSEVERITY_FATAL;
      case LEVEL_ERROR: return LOGSEVERITY_ERROR;
      case LEVEL_WARN: return LOGSEVERITY_WARNING;
      case LEVEL_INFO: return LOGSEVERITY_INFO;
      case LEVEL_DEBUG: return LOGSEVERITY_DEBUG;
      case LEVEL_TRACE: return LOGSEVERITY_DEBUG;
    }
    return LOGSEVERITY_DEFAULT;
  }
 private:
  static void initImpl(int level, FILE* logFile = nullptr);
};

typedef std::chrono::high_resolution_clock Clock;
typedef std::chrono::duration<float, std::micro> Duration;

class Measurer {
public:
  Measurer(const std::string & msg);
  Measurer() : Measurer("") {}
  virtual ~Measurer();

  void append(const std::string & msg);

private:
  const Clock::time_point myStartTime;
  std::string myMsg;
};

class LogNdc {
 public:
  LogNdc(std::string file, std::string func, std::string threadName);
  LogNdc(std::string file,
         std::string func = "",
         int thresholdMcs = -1,
         bool logStart = false,
         bool logFinish = false,
         std::string threadName = "");
  virtual ~LogNdc();

 private:
  const Clock::time_point startTime;
  int thresholdMcs = -1;
  bool logStart = false;
  bool logFinish = false;
};

void setThreadName(std::string name);

#define LNDC() LogNdc ndc(__FILE_NAME__, __FUNCTION__)

#ifdef _WIN32
#define __FILE_NAME__ (strrchr(__FILE__, '/') ? strrchr(__FILE__, '/') + 1 : __FILE__)
#endif

#ifdef __linux__
#define __FILE_NAME__  __FILE__
#endif

#define LNDCT() LogNdc ndc(__FILE_NAME__, __FUNCTION__, 1000)
#define LNDCTT(thresholdMcs) LogNdc ndc(__FILE_NAME__, __FUNCTION__, thresholdMcs)
#define LNDCTTS(thresholdMcs) LogNdc ndc(__FILE_NAME__, __FUNCTION__, thresholdMcs, true)

#define TRACE() LogNdc ndc(__FILE_NAME__, __FUNCTION__, 1000, true, false)

#endif  // JCEF_LOG_H
