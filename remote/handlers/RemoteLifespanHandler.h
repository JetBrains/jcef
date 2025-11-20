#ifndef JCEF_REMOTELIFESPANHANDLER_H
#define JCEF_REMOTELIFESPANHANDLER_H

#include <thrift/Thrift.h>
#include "include/cef_life_span_handler.h"

class ServerHandlerContext;
class RpcExecutor;
class MessageRoutersManager;
class ClientsManager;
class RemoteBrowser;


class RemoteLifespanHandler : public CefLifeSpanHandler {
 public:
  explicit RemoteLifespanHandler(std::shared_ptr<ServerHandlerContext> ctx, std::shared_ptr<MessageRoutersManager> routersManager);

  //
  // All next methods will be called on the UI thread
  //
  bool OnBeforePopup(CefRefPtr<CefBrowser> browser,
                     CefRefPtr<CefFrame> frame,
                     int popup_id,
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

  void addCreating(int bid);
  void removeCreating(int bid);

 private:
  std::shared_ptr<RpcExecutor> myService;
  std::shared_ptr<MessageRoutersManager> myRoutersManager;

  std::mutex myCreatingMutex;
  std::list<int> myCreatingBids;

  IMPLEMENT_REFCOUNTING(RemoteLifespanHandler);
};

#endif  // JCEF_REMOTELIFESPANHANDLER_H
