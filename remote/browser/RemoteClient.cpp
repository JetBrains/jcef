#include "RemoteClient.h"

#include "../ServerApplication.h"
#include "../handlers/RemoteClientHandler.h"
#include "../network/RemoteRequestContextHandler.h"
#include "../router/MessageRoutersManager.h"
#include "RemoteBrowser.h"
#include "../router/RemoteMessageRouter.h"

std::atomic<int> RemoteClient::sNextCid;

int RemoteClient::genNewCid() { return sNextCid.fetch_add(1); }

RemoteClient::RemoteClient(int cid, CefRefPtr<RemoteClientHandler> handler) : myCid(cid), myRemoteClientHandler(handler) {}

RemoteClient::~RemoteClient() {
  close();
}

void RemoteClient::addHandlers(int handlersMask) {
  myRemoteClientHandler->addHandlers(handlersMask);
}

void RemoteClient::removeHandlers(int handlersMask) {
  myRemoteClientHandler->removeHandlers(handlersMask);
}

void RemoteClient::addMessageRouter(RemoteMessageRouter* router) {
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

void RemoteClient::removeMessageRouter(RemoteMessageRouter* router) {
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
    const thrift_codegen::RObject& requestContextHandler
) {
  // TODO: Expose CefRequestContextSettings.
  CefRequestContextSettings settings;
  CefRefPtr<CefRequestContext> requestContext = requestContextHandler.isNull
          ? CefRequestContext::GetGlobalContext()
          : CefRequestContext::CreateContext(settings,new RemoteRequestContextHandler(ctx, requestContextHandler));

  std::shared_ptr<RemoteBrowser> result = RemoteBrowser::create(owner, requestContext);
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
