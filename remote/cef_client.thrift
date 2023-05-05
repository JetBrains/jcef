
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

namespace cpp thrift_codegen
namespace java thrift_codegen

struct CustomScheme {
1: required string schemeName;
2: required i32 options;
}

service ClientHandlers {
   i32 connect()
   oneway void log(1:string msg)

   //
   // CefAppHandler
   //
   list<CustomScheme> getRegisteredCustomSchemes()
   void onContextInitialized()

   //
   // CefRenderHandler
   //
   binary getInfo(1:i32 cid, 2:i32 bid, 3:string request, 4:binary buffer)
   void onPaint(1:i32 cid, 2:i32 bid, 3:bool popup, 4:i32 dirtyRectsCount, 5:string sharedMemName, 6:i64 sharedMemHandle, 7:bool recreateHandle, 8:i32 width, 9:i32 height)

   //
   // CefLifeSpanHandler
   //
   oneway void onBeforePopup(1:i32 cid, 2:i32 bid, 3:string url, 4:bool gesture) // TODO: add other params
   oneway void onAfterCreated(1:i32 cid, 2:i32 bid)
   oneway void doClose(1:i32 cid, 2:i32 bid)
   oneway void onBeforeClose(1:i32 cid, 2:i32 bid)

   //
   // CefLoadHandler
   //
   oneway void onLoadingStateChange(1:i32 cid, 2:i32 bid, 3:bool isLoading, 4:bool canGoBack, 5:bool canGoForward)
   oneway void onLoadStart(1:i32 cid, 2:i32 bid, 3:i32 transition_type)
   oneway void onLoadEnd(1:i32 cid, 2:i32 bid, 3:i32 httpStatusCode)
   oneway void onLoadError(1:i32 cid, 2:i32 bid, 3:i32 errorCode, 4:string errorText, 5:string failedUrl)
}
