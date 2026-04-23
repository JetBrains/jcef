#include "RemoteClient.h"

#include "../ServerApplication.h"
#include "../handlers/RemoteClientHandler.h"
#include "../network/RemoteRequestContextHandler.h"
#include "../network/RemoteRequestContext.h"
#include "../router/MessageRoutersManager.h"
#include "RemoteBrowser.h"
#include "../router/RemoteMessageRouter.h"

namespace {
const bool doTrace = getBoolEnv("CEF_SERVER_TRACE_RemoteClient");
}

std::atomic<int> RemoteClient::sNextCid;

int RemoteClient::genNewCid() { return sNextCid.fetch_add(1); }

RemoteClient::RemoteClient(int cid, CefRefPtr<RemoteClientHandler> handler) : myCid(cid), myRemoteClientHandler(handler) {
  if (doTrace)
    Log::trace("RemoteClient: created cid=%d", cid);
}

RemoteClient::~RemoteClient() {
  if (doTrace)
    Log::trace("RemoteClient: disposed cid=%d", myCid);
  close();
}

void RemoteClient::addHandlers(int handlersMask) {
  myRemoteClientHandler->addHandlers(handlersMask);
}

void RemoteClient::removeHandlers(int handlersMask) {
  myRemoteClientHandler->removeHandlers(handlersMask);
}

void RemoteClient::addMessageRouter(std::shared_ptr<RemoteMessageRouter> router) {
  myRemoteClientHandler->addMessageRouter(router);

  RemoteBrowser::AddMessageRouterConfig(router->getConfig());

  // Update running render-processes.
  CefRefPtr<CefProcessMessage> message = CefProcessMessage::Create("AddMessageRouter");
  CefRefPtr<CefListValue> args = message->GetArgumentList();
  args->SetString(0, router->getConfig().js_query_function);
  args->SetString(1, router->getConfig().js_cancel_function);

  {
    std::unique_lock lock(myMutex);
    for (auto const& kv : myBrowsers) {
      if (kv.second && kv.second->getCefBrowser())
        kv.second->getCefBrowser()->GetMainFrame()->SendProcessMessage(PID_RENDERER, message);
    }
  }
}

void RemoteClient::removeMessageRouter(std::shared_ptr<RemoteMessageRouter> router) {
  myRemoteClientHandler->removeMessageRouter(router);

  RemoteBrowser::RemoveMessageRouterConfig(router->getConfig());

  // Update running render-processes.
  CefRefPtr<CefProcessMessage> message =
    CefProcessMessage::Create("RemoveMessageRouter");
  CefRefPtr<CefListValue> args = message->GetArgumentList();
  args->SetString(0, router->getConfig().js_query_function);
  args->SetString(1, router->getConfig().js_cancel_function);

  {
    std::unique_lock lock(myMutex);
    for (auto const& kv : myBrowsers) {
      if (kv.second && kv.second->getCefBrowser())
        kv.second->getCefBrowser()->GetMainFrame()->SendProcessMessage(PID_RENDERER, message);
    }
  }
}

std::shared_ptr<RemoteBrowser> RemoteClient::createBrowser(
    std::shared_ptr<RemoteClient> owner,
    std::shared_ptr<ServerHandlerContext> ctx,
    std::shared_ptr<RemoteRequestContext> requestContext
) {
  CefRefPtr<CefRequestContext> cefReqCtx = requestContext ? requestContext->getDelegate() : CefRequestContext::GetGlobalContext();
  std::shared_ptr<RemoteBrowser> result = RemoteBrowser::create(owner, cefReqCtx);
  {
    std::unique_lock lock(myMutex);
    myBrowsers[result->getBid()] = result;
  }
  return result;
}

void RemoteClient::close() {
  std::unique_lock lock(myMutex);
  for (auto const& kv : myBrowsers) {
    if (kv.second)
      kv.second->close();
  }
  myBrowsers.clear();
}

std::shared_ptr<RemoteClient> RemoteClient::findByBid(int bid) {
  std::shared_ptr<RemoteBrowser> rb = RemoteBrowser::find(bid);
  return rb ? rb->getOwner() : nullptr;
}

void RemoteClient::eraseBrowser(int bid) {
  std::unique_lock lock(myMutex);
  myBrowsers.erase(bid);
}

std::string RemoteClient::getDebugInfo(int tabs) {
  std::stringstream ss;
  for (int i = 0; i < tabs; ++i) ss << "\t";
  std::unique_lock lock(myMutex);
  ss << "cid=" << myCid << ", RemoteBrowsers [count=" << myBrowsers.size() << "]:" << std::endl;
  for (auto i: myBrowsers)
    ss << i.second->getDebugInfo(tabs + 1);
  return ss.str();
}
