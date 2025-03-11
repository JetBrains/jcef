

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

struct ContextMenuParams {
    1: required i32 x,
    2: required i32 y,
    3: required i32 type_flags,
    4: required string link_url,
    5: required string unfiltered_link_url,
    6: required string source_url,
    7: required bool has_image_contents,
    8: required string title_text,
    9: required string page_url,
    10: required string frame_url,
    11: required string frame_charset,
    12: required i32 media_type,
    13: required i32 media_state_flags,
    14: required string selected_text,
    15: required string misspelled_word,
    16: required bool is_editable,
    17: required bool is_spellcheck_enabled,
    18: required i32 edit_state_flags,
    19: required bool is_custom_menu,
    // not implemented:
    // GetDictionarySuggestions(std::vector<CefString>& suggestions);
}

enum MenuItemType {
  MENUITEMTYPE_NONE = 0,
  MENUITEMTYPE_COMMAND = 1,
  MENUITEMTYPE_CHECK = 2,
  MENUITEMTYPE_RADIO = 3,
  MENUITEMTYPE_SEPARATOR = 4,
  MENUITEMTYPE_SUBMENU = 5
}

// Struct to represent a Menu Item
struct MenuItem {
  1: required i32 command_id = -1;
  2: required string label = "";
  3: required MenuItemType type = 0;
  4: required i32 group_id = -1;
  5: required bool visible = true;
  6: required bool enabled = true;
  7: required bool checked = false;
  8: required list<MenuItem> sub_menu = [];
  // Not supported:
  // optional KeyboardAccelerator accelerator;
  // optional i32 color_text;
  // optional i32 color_background;
  // optional string font_list;
}

service ClientHandlers {
    string echo(1:string msg),
    oneway void log(1: string msg),
    //
    // CefAppHandler
    //
    oneway void AppHandler_OnContextInitialized(),
    //
    // CefRenderHandler
    //
    // TODO: remake logic to avoid non-oneway-void calls:
    //  1. notify server with screen-data changes immediately
    //  2. calculate screen-data directly on server by request from CEF
    Rect         RenderHandler_GetViewRect(1:i32 bid),
    ScreenInfo   RenderHandler_GetScreenInfo(1:i32 bid),
    Point        RenderHandler_GetScreenPoint(1:i32 bid, 2:i32 viewX, 3:i32 viewY),
    void         RenderHandler_OnPaint(1:i32 bid, 2: bool popup, 3:i32 dirtyRectsCount, 4: string sharedMemName, 5: i64 sharedMemHandle, 6: i32 width, 7: i32 height),
    oneway void  RenderHandler_OnPopupShow(1:i32 bid, 2: bool show)
    oneway void  RenderHandler_OnPopupSize(1:i32 bid, 2: Rect rect)
    // TODO: implement
    // RenderHandler_StartDragging(1:i32 bid, CefRefPtr<CefDragData> drag_data, DragOperationsMask allowed_ops, int x, int y)
    // RenderHandler_UpdateDragCursor(1:i32 bid, DragOperation operation)

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
    void        LoadHandler_OnLoadStart(1:i32 bid, 2:shared.RObject frame, 3:i32 transition_type),
    void        LoadHandler_OnLoadEnd(1:i32 bid, 2:shared.RObject frame, 3:i32 httpStatusCode),
    void        LoadHandler_OnLoadError(1:i32 bid, 2:shared.RObject frame, 3:i32 errorCode, 4:string errorText, 5:string failedUrl),
    //
    // CefDisplayHandler
    //
    void        DisplayHandler_OnAddressChange(1:i32 bid, 2:shared.RObject frame, 3:string url),
    oneway void DisplayHandler_OnTitleChange(1:i32 bid, 2:string title),
    bool        DisplayHandler_OnTooltip(1:i32 bid, 2:string text),
    oneway void DisplayHandler_OnStatusMessage(1:i32 bid, 2:string value),
    bool        DisplayHandler_OnConsoleMessage(1:i32 bid, 2:string level, 3:string message, 4: string source, 5: i32 line),
    //
    // CefKeyboardHandler (will be called on the UI thread). TODO: do we really need CefKeyboardHandler in OSR ?
    //
    bool KeyboardHandler_OnPreKeyEvent(1:i32 bid, 2: shared.KeyEvent event) // TODO: support bool* is_keyboard_shortcut
    bool KeyboardHandler_OnKeyEvent(1:i32 bid, 2: shared.KeyEvent event)
    //
    // CefFocusHandler (will be called on the UI thread). TODO: do we really need CefFocusHandler in OSR ?
    //
    oneway void FocusHandler_OnTakeFocus(1:i32 bid, 2: bool next)
    bool        FocusHandler_OnSetFocus(1:i32 bid, 2:string source)
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
    void                   ResourceHandler_Cancel(1:i32 resourceHandler)    // NOTE: can't be oneway (because server can dispose java peer before callback execution)
    string            ResourceRequestHandler_OnResourceRedirect(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:shared.RObject response, 6:string new_url),
    bool              ResourceRequestHandler_OnResourceResponse(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:shared.RObject response),
    void              ResourceRequestHandler_OnResourceLoadComplete(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:shared.RObject response, 6:string status, 7:i64 receivedContentLength),
    bool              ResourceRequestHandler_OnProtocolExecution(1: i32 rrHandler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:bool allowOsExecution),

    //
    // CefContextMenuHandler
    //
    list<MenuItem> ContextMenuHandler_OnBeforeContextMenu(1: i32 bid, 2: shared.RObject frame, 3: ContextMenuParams params, 4: list<MenuItem> menu_model)
    bool ContextMenuHandler_RunContextMenu(1: i32 bid, 2: shared.RObject frame, 3: ContextMenuParams params, 4: list<MenuItem> model, 5: shared.RObject callback)
    bool ContextMenuHandler_OnContextMenuCommand(1: i32 bid, 2: shared.RObject frame, 3: ContextMenuParams params, 4: i32 command_id, 5: i32 event_flags)
    void ContextMenuHandler_OnContextMenuDismissed(1: i32 bid, 2: shared.RObject frame);

    //
    // CefMessageRouter
    //
    bool        MessageRouterHandler_onQuery(1: shared.RObject handler, 2:i32 bid, 3:shared.RObject frame, 4:i64 queryId, 5:string request, 6:bool persistent, 7:shared.RObject queryCallback),
    void        MessageRouterHandler_onQueryCanceled(1: shared.RObject handler, 2:i32 bid, 3:shared.RObject frame, 4:i64 queryId),
    oneway void MessageRouterHandler_Dispose(1: i32 handler),

    //
    // Custom schemes
    //
    shared.RObject SchemeHandlerFactory_CreateHandler(1:i32 schemeHandlerFactory, 2:i32 bid, 3:shared.RObject frame, 4:string scheme_name, 5:shared.RObject request),
    oneway void    SchemeHandlerFactory_Dispose(1:i32 schemeHandlerFactory),

    //
    // CefCompletionCallback
    //
    oneway void CompletionCallback_OnComplete(1:i32 completionCallback), // NOTE: can be oneway (because java peer is disposed on java side after callback execution)

    oneway void IntCallback_OnComplete(1:i32 intCallback, 2:i32 result), // NOTE: can be oneway (because java peer is disposed on java side after callback execution)

    //
    // CefRequestContextHandler
    //
    shared.RObject RequestContextHandler_GetResourceRequestHandler(1:i32 handler, 2:i32 bid, 3:shared.RObject frame, 4:shared.RObject request, 5:bool isNavigation, 6:bool isDownload, 7:string requestInitiator),

    //
    // CefCookieVisitor
    //
    bool        CookieVisitor_Visit(1:i32 visitor, 2:shared.Cookie cookie, 3:i32 count, 4:i32 total),
    oneway void CookieVisitor_Dispose(1:i32 visitor),

    //
    // CefStringVisitor
    //
    void StringVisitor_Visit(1:i32 stringVisitor, 2:string str), // NOTE: can't be oneway (because server can dispose java peer before callback execution)
    oneway void StringVisitor_Dispose(1:i32 stringVisitor),

    //
    // CefDevToolsMessageObserver
    //
    oneway void DevToolsMessageObserver_Dispose(1:i32 observer),
    oneway void DevToolsMessageObserver_OnDevToolsMethodResult(1:i32 observer, 2:i32 bid, 3:i32 messageId, 4:bool success, 5:string result),
    oneway void DevToolsMessageObserver_OnDevToolsEvent(1:i32 observer, 2:i32 bid, 3:string method, 4:string parameters),

    //
    // CefPermissionHandler
    //
    bool PermissionHandler_OnRequestMediaAccessPermission(1:i32 bid, 2:shared.RObject frame, 3:string requesting_origin, 4:i32 requested_permissions, 5:shared.RObject mediaAccessCallback),

    //
    // CefPdfPrintCallback
    //
    oneway void PdfPrintCallback_OnPdfPrintFinished(1:i32 pdfPrintCallback, 2:string path, 3:bool ok),

    //
    // CefRunFileDialogCallback
    //
    oneway void RunFileDialogCallback_OnFileDialogDismissed(1:i32 runFileDialogCallback, 2:list<string> filePaths),
}