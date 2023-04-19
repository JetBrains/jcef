#ifndef IPC_CEFUTILS_H
#define IPC_CEFUTILS_H

#include <string>
#include <memory>
#include <stdexcept>

#include "include/cef_base.h"

class RemoteAppHandler;

bool doLoadCefLibrary();
void doCefInitializeAndRun(RemoteAppHandler * appHandler);

bool isCefInitialized();

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
