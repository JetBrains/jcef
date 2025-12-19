#ifndef JCEF_UTILS_H
#define JCEF_UTILS_H

#include <string>
#include <stdexcept>
#include <memory>
#include <chrono>

namespace utils {
  int GetPid();
  int GetParentPid();
  std::string GetTempFile(const std::string& identifer, bool useParentId);

  std::string demangle(const char* name);

  bool isFileExist(const char* pathname);

  class Measurer {
    const std::chrono::steady_clock::time_point start = std::chrono::steady_clock::now();
    const bool isEnabled;
    const std::string name;
  public:
    Measurer(bool _isEnabled, const std::string & className, const std::string & funcName) : isEnabled(_isEnabled), name(className + "." + funcName) {}
    ~Measurer();
  };
}

template<typename ... Args>
std::string string_format( const std::string& format, Args ... args )
{
  int size_s = std::snprintf( nullptr, 0, format.c_str(), args ... ) + 1;
  if( size_s <= 0 ){ throw std::runtime_error( "Error during formatting." ); }
  auto size = static_cast<size_t>( size_s );
  std::unique_ptr<char[]> buf( new char[ size ] );
  std::snprintf( buf.get(), size, format.c_str(), args ... );
  return std::string( buf.get(), buf.get() + size - 1 );
}

bool getBoolEnv(const std::string & envName, bool defaultVal = false);
long getLongEnv(const std::string & envName, long defVal);

#endif  // JCEF_UTILS_H
