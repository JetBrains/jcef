

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

struct Point {
    1: required i32 x,
    2: required i32 y,
}

struct Rect {
    1: required i32 x,
    2: required i32 y,
    3: required i32 w,
    4: required i32 h,
}

struct ScreenInfo {
    1: required double device_scale_factor,
    2: required i32 depth,
    3: required i32 depth_per_component,
    4: required bool is_monochrome,
    5: required Rect rect,
    6: required Rect available_rect,
}

service ClientHandlers {
    i32 connect(),
    oneway void log(1: string msg),
    //
    // CefAppHandler
    //
    oneway void AppHandler_OnContextInitialized(),
    //
    // CefRenderHandler
    //
    Rect RenderHandler_GetViewRect(1:i32 bid),
    ScreenInfo RenderHandler_GetScreenInfo(1:i32 bid),
    Point RenderHandler_GetScreenPoint(1:i32 bid, 2:i32 viewX, 3:i32 viewY),
    void RenderHandler_OnPaint(1:i32 bid, 2: bool popup, 3:i32 dirtyRectsCount, 4: string sharedMemName, 5: i64 sharedMemHandle, 6: i32 width, 7: i32 height),
    // TODO: implement
    // OnPopupShow(1:i32 bid, bool show)
    // OnPopupSize(1:i32 bid, const CefRect& rect)
    // StartDragging(1:i32 bid, CefRefPtr<CefDragData> drag_data, DragOperationsMask allowed_ops, int x, int y)
    // UpdateDragCursor(1:i32 bid, DragOperation operation)

    //
    //
    // CefLifeSpanHandler
    //
    bool        LifeSpanHandler_OnBeforePopup(1:i32 bid, 2:shared.RObject frame, 3:string url, 4:string frameName, 5:bool gesture), // TODO: add other params
    oneway void LifeSpanHandler_OnAfterCreated(1:i32 bid, 2:i32 nativeBrowserIdentifier),
    bool        LifeSpanHandler_DoClose(1:i32 bid),
    oneway void LifeSpanHandler_OnBeforeClose(1:i32 bid),
    //
    // CefLoadHandler
    //
    oneway void LoadHandler_OnLoadingStateChange(1:i32 bid, 2: bool isLoading, 3:bool canGoBack, 4: bool canGoForward),
    oneway void LoadHandler_OnLoadStart(1:i32 bid, 2:shared.RObject frame, 3:i32 transition_type),
    oneway void LoadHandler_OnLoadEnd(1:i32 bid, 2:shared.RObject frame, 3:i32 httpStatusCode),
    oneway void LoadHandler_OnLoadError(1:i32 bid, 2:shared.RObject frame, 3:i32 errorCode, 4:string errorText, 5:string failedUrl),
    //
    // CefDisplayHandler
    //
    oneway void DisplayHandler_OnAddressChange(1:i32 bid, 2:shared.RObject frame, 3:string url),
    oneway void DisplayHandler_OnTitleChange(1:i32 bid, 2:string title),
    bool        DisplayHandler_OnTooltip(1:i32 bid, 2:string text),
    oneway void DisplayHandler_OnStatusMessage(1:i32 bid, 2:string value),
    bool        DisplayHandler_OnConsoleMessage(1:i32 bid, 2:i32 level, 3:string message, 4: string source, 5: i32 line),
    //
    // CefKeyboardHandler (will be called on the UI thread).
    //
    bool KeyboardHandler_OnPreKeyEvent(1:i32 bid, 2: shared.KeyEvent event) // TODO: support bool* is_keyboard_shortcut
    bool KeyboardHandler_OnKeyEvent(1:i32 bid, 2: shared.KeyEvent event)
    //
    // CefFocusHandler (will be called on the UI thread).
    //
    oneway void FocusHandler_OnTakeFocus(1:i32 bid, 2: bool next)
    bool FocusHandler_OnSetFocus(1:i32 bid, 2:string source)
    oneway void FocusHandler_OnGotFocus(1:i32 bid)
    //
    // CefRequestHandler
    //
    bool           RequestHandler_OnBeforeBrowse(1:i32 bid, 2:shared.RObject frame, 3:shared.RObject request, 4:bool user_gesture, 5:bool is_redirect),
    bool           RequestHandler_OnOpenURLFromTab(1:i32 bid, 2:shared.RObject frame, 3:string target_url, 4:bool user_gesture),
    bool           RequestHandler_GetAuthCredentials(1:i32 bid, 2:string origin_url, 3:bool isProxy, 4: string host, 5: i32 port, 6: string realm, 7: string scheme, 8: shared.RObject authCallback),
    bool           RequestHandler_OnCertificateError(1:i32 bid, 2:string cert_error, 3:string request_url, 4: binary sslInfo, 5: shared.RObject callback),
    oneway void    RequestHandler_OnRenderProcessTerminated(1:i32 bid, 2:string status),
    shared.RObject RequestHandler_GetResourceRequestHandler(1:i32 bid, 2:shared.RObject frame, 3:shared.RObject request, 4:bool isNavigation, 5:bool isDownload, 6:string requestInitiator),
    oneway void       ResourceRequestHandler_Dispose(1: i32 rrHandler),
    shared.RObject    ResourceRequestHandler_GetCookieAccessFilter(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request),
    oneway void           CookieAccessFilter_Dispose(1: i32 filter),
    bool                  CookieAccessFilter_CanSendCookie(1: i32 filter, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:list<string> cookie),
    bool                  CookieAccessFilter_CanSaveCookie(1: i32 filter, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:shared.RObject response, 6:list<string> cookie),
    bool              ResourceRequestHandler_OnBeforeResourceLoad(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request),
    shared.RObject    ResourceRequestHandler_GetResourceHandler(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request),
    oneway void            ResourceHandler_Dispose(1: i32 resourceHandler),
    bool                   ResourceHandler_ProcessRequest(1:i32 resourceHandler, 2:shared.RObject request, 3:shared.RObject callback)
    shared.ResponseHeaders ResourceHandler_GetResponseHeaders(1:i32 resourceHandler, 2:shared.RObject response)
    shared.ResponseData    ResourceHandler_ReadResponse(1:i32 resourceHandler, 2:i32 bytes_to_read, 3:shared.RObject callback)
    oneway void            ResourceHandler_Cancel(1:i32 resourceHandler)
    string            ResourceRequestHandler_OnResourceRedirect(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:shared.RObject response, 6:string new_url),
    bool              ResourceRequestHandler_OnResourceResponse(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:shared.RObject response),
    void              ResourceRequestHandler_OnResourceLoadComplete(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:shared.RObject response, 6:string status, 7:i64 receivedContentLength),
    bool              ResourceRequestHandler_OnProtocolExecution(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:bool allowOsExecution),

    //
    // CefMessageRouter
    //
    bool MessageRouterHandler_onQuery(1: shared.RObject handler, 2:i32 bid, 3:shared.RObject frame, 4:i64 queryId, 5:string request, 6:bool persistent, 7:shared.RObject queryCallback),
    oneway void MessageRouterHandler_onQueryCanceled(1: shared.RObject handler, 2:i32 bid, 3:shared.RObject frame, 4:i64 queryId),
    oneway void MessageRouterHandler_Dispose(1: i32 handler),

    //
    // Custom schemes
    //
    shared.RObject SchemeHandlerFactory_CreateHandler(1:i32 schemeHandlerFactory, 2:i32 bid, 3:shared.RObject frame, 4:string scheme_name, 5:shared.RObject request),
    oneway void SchemeHandlerFactory_Dispose(1:i32 schemeHandlerFactory),

    //
    // CefCompletionCallback
    //
    oneway void CompletionCallback_OnComplete(1:i32 completionCallback),

    //
    // CefRequestContextHandler
    //
    shared.RObject RequestContextHandler_GetResourceRequestHandler(1:i32 handler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:bool isNavigation, 6:bool isDownload, 7:string requestInitiator),

    //
    // CefCookieVisitor
    //
    bool CookieVisitor_Visit(1:i32 visitor, 2:shared.Cookie cookie, 3:i32 count, 4:i32 total),
    oneway void CookieVisitor_Dispose(1:i32 visitor),
}