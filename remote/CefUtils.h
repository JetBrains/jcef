#ifndef IPC_CEFUTILS_H
#define IPC_CEFUTILS_H

#include <string>
#include <memory>
#include <vector>
#include <map>
#include <stdexcept>

#include "include/cef_base.h"

namespace CefUtils {
#if defined(OS_MAC)
  bool doLoadCefLibrary();
#endif
  bool initializeCef(
      std::vector<std::string> switches,
      CefSettings settings,
      std::vector<std::pair<std::string, int>> schemes
  );
  bool initializeCef(std::string paramsFilePath);
  bool initializeCef(int argc, char* argv[]);
  void runCefLoop();

  bool parseSetting(CefSettings & out, const std::string & settingLine);
  bool parseScheme(std::string & name, int & options, const std::string & settingLine);
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

std::string toString(cef_rect_t& rect);
std::string toString(cef_point_t& pt);

#endif //IPC_CEFUTILS_H
