#ifndef JCEF_REMOTELIFESPANHANDLER_H
#define JCEF_REMOTELIFESPANHANDLER_H

#include <thrift/Thrift.h>
#include "include/cef_life_span_handler.h"

class RemoteClientHandler;
class RemoteLifespanHandler : public CefLifeSpanHandler {
 public:
  RemoteLifespanHandler(RemoteClientHandler & owner);
  CefRefPtr<CefBrowser> getBrowser();
  bool OnBeforePopup(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     const CefString& target_url,
                     const CefString& target_frame_name,
                     WindowOpenDisposition target_disposition,
                     bool user_gesture,
                     const CefPopupFeatures& popupFeatures,
                     CefWindowInfo& windowInfo,
                     CefRefPtr<CefClient>& client,
                     CefBrowserSettings& settings,
                     CefRefPtr<CefDictionaryValue>& extra_info,
                     bool* no_javascript_access) override;
  void OnAfterCreated(CefRefPtr<CefBrowser> browser) override;
  bool DoClose(CefRefPtr<CefBrowser> browser) override;
  void OnBeforeClose(CefRefPtr<CefBrowser> browser) override;
 protected:
  RemoteClientHandler & myOwner;
  CefRefPtr<CefBrowser> myBrowser = nullptr;

  void _onThriftException(apache::thrift::TException e);

  IMPLEMENT_REFCOUNTING(RemoteLifespanHandler);
};

#endif  // JCEF_REMOTELIFESPANHANDLER_H
