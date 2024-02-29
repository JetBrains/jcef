#ifndef JCEF_UTILS_H
#define JCEF_UTILS_H

#include <string>
#include <stdexcept>
#include <memory>

namespace utils {
  int GetPid();
  int GetParentPid();
  std::string GetTempFile(const std::string& identifer, bool useParentId);
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

#endif  // JCEF_UTILS_H
