#ifndef JCEF_LOG_H
#define JCEF_LOG_H

#include <log4cxx/logger.h>

#include <chrono>

using namespace ::log4cxx;
class Log {
public:
  static void init();
  
  template <class ... Args>
  static void fatal(const char *const format, Args ... args) {
    log(::log4cxx::Level::getFatal(), format, args...);
  }
  template <class ... Args>
  static void error(const char *const format, Args ... args) {
    log(::log4cxx::Level::getError(), format, args...);
  }
  template <class ... Args>
  static void warn(const char *const format, Args ... args) {
    log(::log4cxx::Level::getWarn(), format, args...);
  }
  template <class ... Args>
  static void info(const char *const format, Args ... args) {
    log(::log4cxx::Level::getInfo(), format, args...);
  }
  template <class ... Args>
  static void debug(const char *const format, Args ... args) {
    log(::log4cxx::Level::getDebug(), format, args...);
  }
  template <class ... Args>
  static void trace(const char *const format, Args ... args) {
    log(::log4cxx::Level::getTrace(), format, args...);
  }

  static void log(const LevelPtr & level, const char *const format, ...);
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

#define LNDC() LogNdc ndc(__FILE_NAME__, __FUNCTION__)

#define LNDCT() LogNdc ndc(__FILE_NAME__, __FUNCTION__, 1000)
#define LNDCTT(thresholdMcs) LogNdc ndc(__FILE_NAME__, __FUNCTION__, thresholdMcs)
#define LNDCTTS(thresholdMcs) LogNdc ndc(__FILE_NAME__, __FUNCTION__, thresholdMcs, true)

#define TRACE() LogNdc ndc(__FILE_NAME__, __FUNCTION__, 1000, true)

#endif  // JCEF_LOG_H
