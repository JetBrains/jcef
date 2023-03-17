
/**
 *  bool        Boolean, one byte
 *  i8 (byte)   Signed 8-bit integer
 *  i16         Signed 16-bit integer
 *  i32         Signed 32-bit integer
 *  i64         Signed 64-bit integer
 *  double      64-bit floating point value
 *  string      String
 *  binary      Blob (byte array)
 *  map<t1,t2>  Map from one type to another
 *  list<t1>    Ordered list of one type
 *  set<t1>     Set of unique elements of one type
 */

namespace cpp remote
namespace java remote

service ClientHandlers {
   i32 connect(),
   oneway void log(1:string msg),

   //
   // CefRenderHandler
   //
   binary getInfo(1:i32 bid, 2:string request, 3:binary buffer),
   void onPaint(1:i32 bid, 2:bool popup, 3:i32 dirtyRectsCount, 4:string sharedMemName, 5:i64 sharedMemHandle, 6:bool recreateHandle, 7:i32 width, 8:i32 height)
}
