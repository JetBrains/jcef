#ifndef JCEF_REMOTEBROWSER_H
#define JCEF_REMOTEBROWSER_H

#include <map>
#include <mutex>

#include "../handlers/SharedBufferManager.h"
#include "include/internal/cef_ptr.h"
#include "include/wrapper/cef_message_router.h"

class CefRequestContext;
class RemoteClient;
class CefBrowser;

class RemoteBrowser {
public:
  static std::shared_ptr<RemoteBrowser> create(std::shared_ptr<RemoteClient> owner, CefRefPtr<CefRequestContext> requestContext);

  std::shared_ptr<RemoteClient> getOwner() const { return myOwner; }

  int getBid() const { return myBid; }
  int getCid() const;
  CefRefPtr<CefRequestContext> getRequestContext() const;

  CefRefPtr<CefBrowser> getCefBrowser() const;

  void startNativeBrowserCreation(const std::string& url);
  void openDevTools(int x, int y);

  void close();
  bool isClosing() const { return myIsClosing; }

  SharedBufferManager &page() { return myPage; }
  SharedBufferManager &popup() { return myPopup; }

  static std::shared_ptr<RemoteBrowser> find(int bid);
  static std::shared_ptr<RemoteBrowser> findByCefBrowser(CefRefPtr<CefBrowser> browser);

  static bool closeAllBrowsers(); // returns true when no browsers presented (i.e. all browsers are already closed)
  static std::vector<int> enumAllBrowsers();
  static unsigned int getAllBrowsersCount();

  static void AddMessageRouterConfig(const CefMessageRouterConfig& cfg);
  static void RemoveMessageRouterConfig(const CefMessageRouterConfig& cfg);

  explicit RemoteBrowser(int bid, std::shared_ptr<RemoteClient> owner, CefRefPtr<CefRequestContext> requestContext); // TODO: make private

  std::string getDebugInfo(int tabs);
private:
  const int myBid;
  const std::shared_ptr<RemoteClient> myOwner;
  const CefRefPtr<CefRequestContext> myRequestContext;

  SharedBufferManager myPage;
  SharedBufferManager myPopup;
  CefRefPtr<CefBrowser> myCefBrowser;
  bool myIsClosing = false;

  static std::mutex ourBid2BrowserMutex;
  static std::map<int, std::shared_ptr<RemoteBrowser>> ourBid2Browser;

  static std::mutex ourCef2RemoteMutex;
  static std::map<int, std::shared_ptr<RemoteBrowser>> ourCef2Remote;

  friend class RemoteLifespanHandler;
  friend class RemoteClient;
  void onBeforeClose();
  void setCefBrowser(CefRefPtr<CefBrowser> browser);

  static void linkCefBrowser(CefRefPtr<CefBrowser> browser, std::shared_ptr<RemoteBrowser> remoteBrowser);
  static void unlinkCefBrowser(CefRefPtr<CefBrowser> browser);
};

#define FIND_BID_OR_RETURN()                               \
if (!browser)                                              \
  return;                                                  \
const auto & rb = RemoteBrowser::findByCefBrowser(browser);\
if (!rb)                                                   \
  return;                                                  \
const int bid = rb->getBid()

#define FIND_BID_OR_RETURN_VAL(x)                          \
if (!browser)                                              \
  return x;                                                \
const auto & rb = RemoteBrowser::findByCefBrowser(browser);\
if (!rb)                                                   \
  return x;                                                \
const int bid = rb->getBid()

#endif //JCEF_REMOTEBROWSER_H