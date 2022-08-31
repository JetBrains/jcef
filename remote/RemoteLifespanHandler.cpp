#include "RemoteLifespanHandler.h"

RemoteLifespanHandler::RemoteLifespanHandler() {}

bool RemoteLifespanHandler::OnBeforePopup(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    const CefString& target_url,
    const CefString& target_frame_name,
    CefLifeSpanHandler::WindowOpenDisposition target_disposition,
    bool user_gesture,
    const CefPopupFeatures& popupFeatures,
    CefWindowInfo& windowInfo,
    CefRefPtr<CefClient>& client,
    CefBrowserSettings& settings,
    CefRefPtr<CefDictionaryValue>& extra_info,
    bool* no_javascript_access) {
  return CefLifeSpanHandler::OnBeforePopup(
      browser, frame, target_url, target_frame_name, target_disposition,
      user_gesture, popupFeatures, windowInfo, client, settings, extra_info,
      no_javascript_access);
}

void RemoteLifespanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser) {
  myBrowser = browser;
  CefLifeSpanHandler::OnAfterCreated(browser);
}

bool RemoteLifespanHandler::DoClose(CefRefPtr<CefBrowser> browser) {
  myBrowser = nullptr;
  return CefLifeSpanHandler::DoClose(browser);
}

void RemoteLifespanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser) {
  CefLifeSpanHandler::OnBeforeClose(browser);
}
CefRefPtr<CefBrowser> RemoteLifespanHandler::getBrowser() {
  return myBrowser;
}
