

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
namespace java com.jetbrains.cef.remote.thrift_codegen

service Server {
    // Pass isMaster=true to mark client as 'master'.
    // The server will stops itself after last master-client disconnected.
    i32 connect(1: string backwardConnectionPipe, 2: bool isMaster),
    i32 connectTcp(1: i32 backwardConnectionPort, 2: bool isMaster),
    oneway void log(1: string msg),
    string echo(1: string msg),
    string version(),
    string state(),
    oneway void stop(),

    //
    // CefBrowser
    //
    i32 Browser_Create(1: i32 cid, 2: i32 handlersMask, 3:shared.RObject requestContextHandler),
    oneway void Browser_StartNativeCreation(1: i32 bid, 2: string url),
    oneway void Browser_Close(1: i32 bid),

    oneway void Browser_Reload(1: i32 bid),
    oneway void Browser_ReloadIgnoreCache(1: i32 bid),
    oneway void Browser_LoadURL(1: i32 bid, 2: string url),
    string      Browser_GetURL(1: i32 bid),
    oneway void Browser_ExecuteJavaScript(1: i32 bid, 2: string code, 3: string url, 4: i32 line),
    oneway void Browser_WasResized(1: i32 bid), // The browser will then call CefRenderHandler#GetViewRect to update the size of view area with the new values.
    oneway void Browser_NotifyScreenInfoChanged(1: i32 bid),  // The browser will then call CefRenderHandler#GetScreenInfo to update the screen information with the new values.
    oneway void Browser_SendKeyEvent(1: i32 bid, 2: i32 event_type, 3: i32 modifiers, 4: i16 key_char, 5: i64 scanCode, 6: i32 key_code),
    oneway void Browser_SendMouseEvent(1: i32 bid, 2: i32 event_type, 3: i32 x, 4: i32 y, 5: i32 modifiers, 6: i32 click_count, 7: i32 button),
    oneway void Browser_SendMouseWheelEvent(1: i32 bid, 2: i32 scroll_type, 3: i32 x, 4: i32 y, 5: i32 modifiers, 6: i32 delta, 7: i32 units_to_scroll),
    bool        Browser_CanGoForward(1: i32 bid),
    bool        Browser_CanGoBack(1: i32 bid),
    oneway void Browser_GoBack(1: i32 bid),
    oneway void Browser_GoForward(1: i32 bid),
    bool        Browser_IsLoading(1: i32 bid),
    oneway void Browser_StopLoad(1: i32 bid),
    i32         Browser_GetFrameCount(1: i32 bid),
    bool        Browser_IsPopup(1: i32 bid),
    bool        Browser_HasDocument(1: i32 bid),
    oneway void Browser_ViewSource(1: i32 bid),
    oneway void Browser_GetSource(1: i32 bid, 2:shared.RObject stringVisitor),
    oneway void Browser_GetText(1: i32 bid, 2:shared.RObject stringVisitor),
    oneway void Browser_SetFocus(1: i32 bid, 2:bool enable),
    double      Browser_GetZoomLevel(1: i32 bid),
    oneway void Browser_SetZoomLevel(1: i32 bid, 2:double val),
    oneway void Browser_StartDownload(1: i32 bid, 2:string url),
    oneway void Browser_Find(1: i32 bid, 2:string searchText, 3:bool forward, 4:bool matchCase, 5:bool findNext),
    oneway void Browser_StopFinding(1: i32 bid, 2:bool clearSelection),
    oneway void Browser_ReplaceMisspelling(1: i32 bid, 2:string word),
    oneway void Browser_SetFrameRate(1: i32 bid, 2:i32 val),

    //
    // CefFrame
    //
    oneway void Frame_ExecuteJavaScript(1:i32 frameId, 2:string code, 3:string url, 4:i32 line),

    //
    // CefRequest
    //
    void Request_Update(1: shared.RObject request),
    shared.PostData Request_GetPostData(1: shared.RObject request),
    void Request_SetPostData(1: shared.RObject request, 2: shared.PostData postData),
    string Request_GetHeaderByName(1: shared.RObject request, 2: string name),
    void Request_SetHeaderByName(1: shared.RObject request, 2: string name, 3: string value, 4: bool overwrite),
    map<string, string> Request_GetHeaderMap(1: shared.RObject request),                                                                                 // TODO: support multimaps
    void Request_SetHeaderMap(1: shared.RObject request, 2: map<string, string> headerMap),
    void Request_Set(1: shared.RObject request, 2: string url, 3: string method, 4: shared.PostData postData, 5: map<string, string> headerMap),
    //
    // CefResponse
    //
    void Response_Update(1: shared.RObject response),
    string Response_GetHeaderByName(1: shared.RObject response, 2: string name),
    void Response_SetHeaderByName(1: shared.RObject response, 2: string name, 3: string value, 4: bool overwrite),
    map<string, string> Response_GetHeaderMap(1: shared.RObject response),                                                                               // TODO: support multimaps
    void Response_SetHeaderMap(1: shared.RObject response, 2: map<string, string> headerMap),
    //
    // Callback
    //
    oneway void Callback_Dispose(1: shared.RObject callback),
    oneway void Callback_Continue(1: shared.RObject callback),
    oneway void Callback_Cancel(1: shared.RObject callback),
    //
    // CefAuthCallback
    //
    oneway void AuthCallback_Dispose(1: shared.RObject authCallback),
    oneway void AuthCallback_Continue(1: shared.RObject authCallback, 2: string username, 3: string password),
    oneway void AuthCallback_Cancel(1: shared.RObject authCallback),
    //
    // CefMessageRouter
    //
    shared.RObject MessageRouter_Create(1: string query, 2: string cancel),
    oneway void MessageRouter_Dispose(1: shared.RObject msgRouter),
    void MessageRouter_AddMessageRouterToBrowser(1: shared.RObject msgRouter, 2: i32 bid),
    void MessageRouter_RemoveMessageRouterFromBrowser(1: shared.RObject msgRouter, 2: i32 bid),
    void MessageRouter_AddHandler(1: shared.RObject msgRouter, 2: shared.RObject handler, 3: bool first),
    void MessageRouter_RemoveHandler(1: shared.RObject msgRouter, 2: shared.RObject handler),
    void MessageRouter_CancelPending(1: shared.RObject msgRouter, 2: i32 bid, 3: shared.RObject handler),
    oneway void QueryCallback_Dispose(1: shared.RObject qcallback),
    oneway void QueryCallback_Success(1: shared.RObject qcallback, 2: string response),
    oneway void QueryCallback_Failure(1: shared.RObject qcallback, 2: i32 error_code, 3: string error_message),

    //
    // Custom schemes
    //
    oneway void SchemeHandlerFactory_Register(1:string schemeName, 2:string domainName, 3:shared.RObject schemeHandlerFactory),
    oneway void ClearAllSchemeHandlerFactories(),

    //
    // CefRequestContext
    //
    oneway void RequestContext_ClearCertificateExceptions(1:i32 bid, 2:shared.RObject completionCallback),
    oneway void RequestContext_CloseAllConnections(1:i32 bid, 2:shared.RObject completionCallback),

    shared.RObject CookieManager_Create(),
    oneway void CookieManager_Dispose(1:shared.RObject cookieManager),
    bool CookieManager_VisitAllCookies(1:shared.RObject cookieManager, 2:shared.RObject visitor),
    bool CookieManager_VisitUrlCookies(1:shared.RObject cookieManager, 2:shared.RObject visitor, 3:string url, 4:bool includeHttpOnly),
    bool CookieManager_SetCookie(1:shared.RObject cookieManager, 2:string url, 3:shared.Cookie cookie),
    bool CookieManager_DeleteCookies(1:shared.RObject cookieManager, 2:string url, 3:string cookieName),
    bool CookieManager_FlushStore(1:shared.RObject cookieManager, 2:shared.RObject completionCallback),
}