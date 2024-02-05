#ifndef JCEF_REMOTELIFESPANHANDLER_H
#define JCEF_REMOTELIFESPANHANDLER_H

#include <thrift/Thrift.h>
#include "include/cef_life_span_handler.h"
#include <functional>

class RemoteClientHandler;
class RpcExecutor;
class MessageRoutersManager;

class RemoteLifespanHandler : public CefLifeSpanHandler {
 public:
  explicit RemoteLifespanHandler(int bid, std::shared_ptr<RpcExecutor> service, std::shared_ptr<MessageRoutersManager> routersManager, std::function<void(int)> onCloseCallback);
  CefRefPtr<CefBrowser> getBrowser();
  //
  // All next methods will be called on the UI thread
  //
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

 private:
  const int myBid;
  const std::function<void(int)> myOnClosedCallback;
  std::shared_ptr<RpcExecutor> myService;
  std::shared_ptr<MessageRoutersManager> myRoutersManager;
  CefRefPtr<CefBrowser> myBrowser = nullptr;

  IMPLEMENT_REFCOUNTING(RemoteLifespanHandler);
};

#endif  // JCEF_REMOTELIFESPANHANDLER_H
