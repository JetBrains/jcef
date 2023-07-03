#ifndef JCEF_LOG_H
#define JCEF_LOG_H

#include <chrono>
#include <string>

const int LEVEL_FATAL = 10;
const int LEVEL_ERROR = 9;
const int LEVEL_WARN = 8;
const int LEVEL_INFO = 7;
const int LEVEL_DEBUG = 6;
const int LEVEL_TRACE = 5;

class Log {
public:
  static void init(int level = LEVEL_INFO);
  
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

#define LNDCT() LogNdc ndc(__FILE_NAME__, __FUNCTION__, 1000)
#define LNDCTT(thresholdMcs) LogNdc ndc(__FILE_NAME__, __FUNCTION__, thresholdMcs)
#define LNDCTTS(thresholdMcs) LogNdc ndc(__FILE_NAME__, __FUNCTION__, thresholdMcs, true)

#define TRACE() LogNdc ndc(__FILE_NAME__, __FUNCTION__, 1000, true, false)

#endif  // JCEF_LOG_H
