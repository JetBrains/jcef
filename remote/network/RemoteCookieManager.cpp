#include "RemoteCookieManager.h"

RemoteCookieManager::RemoteCookieManager(std::shared_ptr<RpcExecutor> service, int id, CefRefPtr<CefCookieManager> delegate)
    : RemoteServerObject<RemoteCookieManager, CefCookieManager>(id, delegate), myService(service)
{
  // LogNdc ndc("Create cookie manager", "", 0, true);
}

RemoteCookieManager * RemoteCookieManager::create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefCookieManager> delegate) {
  return FACTORY.create([&](int id) -> RemoteCookieManager* {return new RemoteCookieManager(service, id, delegate);});
}