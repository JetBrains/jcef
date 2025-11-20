#include "RemoteCookieManager.h"

RemoteCookieManager::RemoteCookieManager(int id, std::shared_ptr<RpcExecutor> service, CefRefPtr<CefCookieManager> delegate)
    : RemoteServerObject<RemoteCookieManager, CefCookieManager>(id, delegate), myService(service) {}

std::shared_ptr<RemoteCookieManager> RemoteCookieManager::create(std::shared_ptr<RpcExecutor> service, CefRefPtr<CefCookieManager> delegate) {
  return FACTORY.create(service, delegate);
}