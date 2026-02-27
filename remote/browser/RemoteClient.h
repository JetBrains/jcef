#ifndef JCEF_REMOTECLIENT_H
#define JCEF_REMOTECLIENT_H
#include <map>
#include <memory>
#include <mutex>
#include <vector>

#include "include/internal/cef_ptr.h"

class RemoteMessageRouter;
class MessageRoutersManager;
class CefRequestContext;

namespace thrift_codegen {
    class RObject;
}

class RemoteBrowser;
class CefBrowser;
class ServerHandlerContext;
class RemoteClientHandler;
class RemoteRequestContext;

class RemoteClient {
public:
    explicit RemoteClient(int cid, CefRefPtr<RemoteClientHandler> handler);
    ~RemoteClient();

    int getCid() const { return myCid; }

    void addHandlers(int handlersMask);
    void removeHandlers(int handlersMask);

    void addMessageRouter(std::shared_ptr<RemoteMessageRouter> router);
    void removeMessageRouter(std::shared_ptr<RemoteMessageRouter> router);

    std::shared_ptr<RemoteBrowser> createBrowser(std::shared_ptr<RemoteClient> owner,
                                                 std::shared_ptr<ServerHandlerContext> ctx,
                                                 std::shared_ptr<RemoteRequestContext> requestContext);

    void close();
    static std::shared_ptr<RemoteClient> findByBid(int bid);

    static int genNewCid();

    std::string getDebugInfo(int tabs);
private:
    const int myCid;
    const CefRefPtr<RemoteClientHandler> myRemoteClientHandler;

    std::mutex myMutex;
    std::map<int, std::shared_ptr<RemoteBrowser>> myBrowsers;

    friend class RemoteBrowser;
    void eraseBrowser(int bid);

    static std::atomic<int> sNextCid;
};


#endif //JCEF_REMOTECLIENT_H