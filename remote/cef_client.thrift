
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
   // TODO: implement: OnPopupShow, OnPopupSize, StartDragging, UpdateDragCursor
   binary getInfo(1:i32 bid, 2:string request, 3:binary buffer)
   void onPaint(1:i32 bid, 2:bool popup, 3:i32 dirtyRectsCount, 4:string sharedMemName, 5:i64 sharedMemHandle, 6:bool recreateHandle, 7:i32 width, 8:i32 height)

   //
   // CefLifeSpanHandler
   //
   oneway void onBeforePopup(1:i32 bid, 2:string url, 3:string frameName, 4:bool gesture) // TODO: add other params
   oneway void onAfterCreated(1:i32 bid)
   oneway void doClose(1:i32 bid)
   oneway void onBeforeClose(1:i32 bid)

   //
   // CefLoadHandler
   //
   oneway void onLoadingStateChange(1:i32 bid, 2:bool isLoading, 3:bool canGoBack, 4:bool canGoForward)
   oneway void onLoadStart(1:i32 bid, 2:i32 transition_type)
   oneway void onLoadEnd(1:i32 bid, 2:i32 httpStatusCode)
   oneway void onLoadError(1:i32 bid, 2:i32 errorCode, 3:string errorText, 4:string failedUrl)
}
