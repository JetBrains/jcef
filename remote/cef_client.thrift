
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

include "shared.thrift"

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
   // TODO: support frame argument in all handlers
   //

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

   //
   // CefDisplayHandler
   //
   oneway void onAddressChange(1:i32 bid, 2:string url)
   oneway void onTitleChange(1:i32 bid, 2:string title)
   bool onTooltip(1:i32 bid, 2:string text)
   oneway void onStatusMessage(1:i32 bid, 2:string value)
   bool onConsoleMessage(1:i32 bid, 2:i32 level, 3:string message, 4:string source, 5:i32 line)
   
   //
   // CefRequestHandler
   //
   bool RequestHandler_OnBeforeBrowse(1:i32 bid, 2:shared.RObject request, 3:bool user_gesture, 4:bool is_redirect)
   bool RequestHandler_OnOpenURLFromTab(1:i32 bid, 2:string target_url, 3:bool user_gesture)
   bool RequestHandler_GetAuthCredentials(1:i32 bid, 2:string origin_url, 3:bool isProxy, 4:string host, 5:i32 port, 6:string realm, 7:string scheme, 8:shared.RObject authCallback)
   bool RequestHandler_OnCertificateError(1:i32 bid, 2:string cert_error, 3:string request_url, 4:shared.RObject sslInfo, 5:shared.RObject callback)
   oneway void RequestHandler_OnRenderProcessTerminated(1:i32 bid, 2:string status)

   shared.RObject RequestHandler_GetResourceRequestHandler(1:i32 bid, 2:shared.RObject request, 3:bool isNavigation, 4:bool isDownload, 5:string requestInitiator)
      oneway void ResourceRequestHandler_Dispose(1:i32 rrHandler)
      shared.RObject ResourceRequestHandler_GetCookieAccessFilter(1:i32 rrHandler, 2:i32 bid, 3:shared.RObject request)
         oneway void CookieAccessFilter_Dispose(1:i32 filter)
         bool CookieAccessFilter_CanSendCookie(1:i32 filter, 2:i32 bid, 3:shared.RObject request, 4:list<string> cookie)
         bool CookieAccessFilter_CanSaveCookie(1:i32 filter, 2:i32 bid, 3:shared.RObject request, 4:shared.RObject response, 5:list<string> cookie)
      bool ResourceRequestHandler_OnBeforeResourceLoad(1:i32 rrHandler, 2:i32 bid, 3:shared.RObject request)
      shared.RObject ResourceRequestHandler_GetResourceHandler(1:i32 rrHandler, 2:i32 bid, 3:shared.RObject request)
         oneway void ResourceHandler_Dispose(1:i32 resourceHandler)
         // TODO: implement
         //bool ResourceHandler_Open(1:i32 resourceHandler, 2:shared.RObject request, bool& handle_request, shared.RObject callback)
         //bool ResourceHandler_ProcessRequest(1:i32 resourceHandler, 2:shared.RObject request, shared.RObject callback)
         //string ResourceHandler_GetResponseHeaders(1:i32 resourceHandler, 2:shared.RObject response, int64& response_length, string redirectUrl)
         //bool ResourceHandler_Skip(int64 bytes_to_skip, int64& bytes_skipped, CefRefPtr<CefResourceSkipCallback> callback)
         //bool ResourceHandler_Read(void* data_out, int bytes_to_read, int& bytes_read, CefRefPtr<CefResourceReadCallback> callback)
         //bool ResourceHandler_ReadResponse(void* data_out, int bytes_to_read, int& bytes_read, shared.RObject callback)
         //void ResourceHandler_Cancel()
      string ResourceRequestHandler_OnResourceRedirect(1:i32 rrHandler, 2:i32 bid, 3:shared.RObject request, 4:shared.RObject response, 5:string new_url)
      bool ResourceRequestHandler_OnResourceResponse(1:i32 rrHandler, 2:i32 bid, 3:shared.RObject request, 4:shared.RObject response)
      void ResourceRequestHandler_OnResourceLoadComplete(1:i32 rrHandler, 2:i32 bid, 3:shared.RObject request, 4:shared.RObject response, 5:string status, 6:i64 receivedContentLength)
      bool ResourceRequestHandler_OnProtocolExecution(1:i32 rrHandler, 2:i32 bid, 3:shared.RObject request, 4:bool allowOsExecution)
}
