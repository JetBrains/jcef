#include "RemoteBrowser.h"
#include "RemoteClient.h"
#include "../handlers/RemoteLifespanHandler.h"
#include "../router/MessageRoutersManager.h"
#include "../handlers/RemoteClientHandler.h"

#include "include/base/cef_callback.h"
#include "include/cef_app.h"
#include "include/cef_task.h"
#include "include/wrapper/cef_closure_task.h"
#include "include/cef_browser.h"

std::mutex RemoteBrowser::ourBid2BrowserMutex;
std::map<int, std::shared_ptr<RemoteBrowser>> RemoteBrowser::ourBid2Browser;
std::mutex RemoteBrowser::ourCef2RemoteMutex;
std::map<int, std::shared_ptr<RemoteBrowser>> RemoteBrowser::ourCef2Remote;

RemoteBrowser::RemoteBrowser(int bid, std::shared_ptr<RemoteClient> owner,
                             CefRefPtr<CefRequestContext> requestContext) : myBid(bid), myOwner(owner),
                                                                            myRequestContext(requestContext),
                                                                            myPage(bid, "page"), myPopup(bid, "popup") {
}

std::shared_ptr<RemoteBrowser> RemoteBrowser::create(std::shared_ptr<RemoteClient> owner, CefRefPtr<CefRequestContext> requestContext) {
    std::shared_ptr<RemoteBrowser> result;
    {
        std::unique_lock lock(ourBid2BrowserMutex);
        static int sBid = 0;
        int bid = sBid++;
        ourBid2Browser[bid] = result = std::make_shared<RemoteBrowser>(bid, owner, requestContext);
    }
    return result;
}

int RemoteBrowser::getCid() const { return myOwner->getCid(); }

CefRefPtr<CefRequestContext> RemoteBrowser::getRequestContext() const { return myRequestContext; }

CefRefPtr<CefBrowser> RemoteBrowser::getCefBrowser() const { return myCefBrowser; }

void RemoteBrowser::setCefBrowser(CefRefPtr<CefBrowser> browser) {
    myCefBrowser = browser;
}

void RemoteBrowser::linkCefBrowser(CefRefPtr<CefBrowser> browser, std::shared_ptr<RemoteBrowser> remoteBrowser) {
    std::unique_lock lock(ourCef2RemoteMutex);
    ourCef2Remote[browser->GetIdentifier()] = remoteBrowser;
}

void RemoteBrowser::unlinkCefBrowser(CefRefPtr<CefBrowser> browser) {
    std::unique_lock lock(ourCef2RemoteMutex);
    ourCef2Remote.erase(browser->GetIdentifier());
}

std::shared_ptr<RemoteBrowser> RemoteBrowser::findByCefBrowser(CefRefPtr<CefBrowser> browser) {
    if (!browser)
        return nullptr;

    std::unique_lock lock(ourCef2RemoteMutex);
    auto i = ourCef2Remote.find(browser->GetIdentifier());
    return i == ourCef2Remote.end() ? nullptr : i->second;
}

namespace {
  CefRefPtr<CefListValue> GetAllMessageRouterConfigs();

  // Should be called on UI thread
  void createCefBrowserImpl(
      int cid, int bid,
      CefRefPtr<RemoteClientHandler> clienthandler,
      CefRefPtr<CefRequestContext> requestContext,
      std::string url,
      std::function<void(int)> onCreationFailed
  ) {
    CefBrowserSettings settings; // TODO: get real CefBrowserSettings from java
    CefWindowInfo windowInfo;
    windowInfo.SetAsWindowless(0);
    // JCEF requires Alloy runtime style for "normal" browsers in order for them
    // to be integratable into Java UI.
    windowInfo.runtime_style = CEF_RUNTIME_STYLE_ALLOY;

    CefRefPtr<CefDictionaryValue> extra_info;
    auto router_configs = GetAllMessageRouterConfigs();
    if (router_configs) {
      // Send the message router config to CefHelperApp::OnBrowserCreated.
      extra_info = CefDictionaryValue::Create();
      extra_info->SetList("router_configs", router_configs);
    }

    RemoteLifespanHandler* lsh = (RemoteLifespanHandler*)clienthandler->GetLifeSpanHandler().get();
    lsh->addCreating(bid);

    //Log::trace( "CefBrowserHost::CreateBrowser cid=%d, bid=%d", cid, bid);
    bool result = CefBrowserHost::CreateBrowser(windowInfo, clienthandler, url,
                                                settings, extra_info, requestContext);
    if (!result) {
      Log::error( "Failed to create browser with cid=%d, bid=%d", cid, bid);
      lsh->removeCreating(bid);
      onCreationFailed(bid);
    }
  }

  // Should be called on UI thread
  void openDevToolsPopupImpl(
      int bid,
      CefRefPtr<CefBrowser> parentBrowser,
      CefPoint inspectAt
  ) {
    if (!parentBrowser)
      return;

    Log::trace( "ShowDevTools: bid=%d, pt=(%d,%d)", bid, inspectAt.x, inspectAt.y);
    CefWindowInfo windowInfo;
    CefBrowserSettings settings; // TODO: get real CefBrowserSettings from java
    parentBrowser->GetHost()->ShowDevTools(windowInfo, nullptr, settings, inspectAt);
  }
}

void RemoteBrowser::startNativeBrowserCreation(const std::string & url) {
    std::function remove = [=](int bid){
        myOwner->eraseBrowser(bid);
        std::unique_lock lock(ourBid2BrowserMutex);
        ourBid2Browser.erase(bid);
    };
    if (CefCurrentlyOn(TID_UI)) {
        createCefBrowserImpl(getCid(), myBid, myOwner->myRemoteClientHandler, myRequestContext, url, remove);
    } else {
        CefPostTask(TID_UI, base::BindOnce(&createCefBrowserImpl, getCid(), myBid, myOwner->myRemoteClientHandler, myRequestContext, url, remove));
    }
}

void RemoteBrowser::openDevTools(int x, int y) {
    if (!myCefBrowser) {
        Log::error("Can't open dev-tools for bid=%d, because native CefBrowser wasn't created yet.", myBid);
        return;
    }

    if (CefCurrentlyOn(TID_UI)) {
        openDevToolsPopupImpl(myBid, myCefBrowser, CefPoint(x, y));
    } else {
        CefPostTask(TID_UI, base::BindOnce(&openDevToolsPopupImpl, myBid, myCefBrowser, CefPoint(x, y)));
    }
}

void RemoteBrowser::close() {
    if (myIsClosing)
        return;

    //Log::trace("Scheduled closing native browser, bid=%d", myBid);
    myIsClosing = true;
    if (myCefBrowser != nullptr)
        myCefBrowser->GetHost()->CloseBrowser(true);
}

std::shared_ptr<RemoteBrowser> RemoteBrowser::find(int bid) {
    std::unique_lock lock(ourBid2BrowserMutex);
    const auto & i = ourBid2Browser.find(bid);
    return i == ourBid2Browser.end() ? nullptr : i->second;
}

std::vector<int> RemoteBrowser::enumAllBrowsers() {
  std::unique_lock lock(ourBid2BrowserMutex);
  std::vector<int> result;
  for (auto const& rc : ourBid2Browser) {
    std::shared_ptr<RemoteBrowser> b = rc.second;
    if (b)
      result.push_back(b->getBid());
  }
  return result;
}

unsigned int RemoteBrowser::getAllBrowsersCount() {
    std::unique_lock lock(ourBid2BrowserMutex);
    return (unsigned int)ourBid2Browser.size();
}

bool RemoteBrowser::closeAllBrowsers() {
  bool isEmpty = true;
  std::unique_lock lock(ourBid2BrowserMutex);
  for (auto const& rc : ourBid2Browser) {
    std::shared_ptr<RemoteBrowser> b = rc.second;
    if (b) {
      b->close();
      isEmpty = false;
    }
  }
  return isEmpty;
}

void RemoteBrowser::onBeforeClose() {
    myCefBrowser = nullptr;
    myOwner->eraseBrowser(myBid);

    // Remove the link from a static map
    std::unique_lock lock(ourBid2BrowserMutex);
    ourBid2Browser.erase(myBid);
}

namespace {
    // comparator to check if configuration values are the same
    struct cmpCfg {
        bool operator()(const CefMessageRouterConfig& lValue,
                        const CefMessageRouterConfig& rValue) const {
            std::less<std::string> comp;
            return comp(lValue.js_query_function.ToString(),
                        rValue.js_query_function.ToString());
        }
    };
    std::set<CefMessageRouterConfig, cmpCfg> router_cfg_;
    base::Lock router_cfg_lock_;

    CefRefPtr<CefListValue> GetAllMessageRouterConfigs() {
        int idx = 0;
        static std::set<CefMessageRouterConfig, cmpCfg>::iterator iter;

        base::AutoLock lock_scope(router_cfg_lock_);
        if (router_cfg_.empty())
            return nullptr;

        // Configuration pased to CefHelperApp::OnBrowserCreated.
        auto router_configs = CefListValue::Create();
        for (iter = router_cfg_.begin(); iter != router_cfg_.end(); ++iter) {
            CefRefPtr<CefDictionaryValue> dict = CefDictionaryValue::Create();
            dict->SetString("js_query_function", iter->js_query_function);
            dict->SetString("js_cancel_function", iter->js_cancel_function);
            router_configs->SetDictionary(idx, dict);
            idx++;
        }

        return router_configs;
    }
}

// static
void RemoteBrowser::AddMessageRouterConfig(const CefMessageRouterConfig& cfg) {
    base::AutoLock lock_scope(router_cfg_lock_);
    router_cfg_.insert(cfg);
}

// static
void RemoteBrowser::RemoveMessageRouterConfig(const CefMessageRouterConfig& cfg) {
    base::AutoLock lock_scope(router_cfg_lock_);
    router_cfg_.erase(cfg);
}
