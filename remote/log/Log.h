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

class Measurer {
public:
  Measurer(const std::string & msg);
  virtual ~Measurer();

  void append(const std::string & msg);

private:
  const std::chrono::microseconds myStartTime;
  std::string myMsg;
};

class LogNdc {
 public:
  LogNdc(std::string msg);
  virtual ~LogNdc();
};

#endif  // JCEF_LOG_H
