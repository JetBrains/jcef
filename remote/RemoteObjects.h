#ifndef JCEF_REMOTEOBJECTS_H
#define JCEF_REMOTEOBJECTS_H
#include <mutex>
#include "Utils.h"
#include "RpcExecutor.h"
#include "include/internal/cef_ptr.h"
#include "log/Log.h"
#include "ServerHandlerContext.h"
#include "DebugInfo.h"

template <class T>
class ServerObjectsFactory {
 public:
  ServerObjectsFactory() : myTemplateName(utils::demangle(typeid(T).name())) {
    DebugInfo::addInfoProvider([&]() -> std::string {
      std::unique_lock lock(myMapMutex);
      return string_format("Factory<%s>: size=%d", myTemplateName.c_str(), myItems.size());
    });
    const std::string prefix = "CEF_SERVER_TRACE_FACTORY_";
    myIsTraceEnabled = getBoolEnv(prefix + myTemplateName, false);
  }

  template<typename... Args>
  std::shared_ptr<T> create(Args... ctorArgs) {
    if (myIsTraceEnabled) Log::trace("[%s] create", myTemplateName.c_str());

    int newId;
    {
      std::unique_lock idLock(myIdMutex);
      static int id = 0;
      newId = id++;
    }
    std::shared_ptr<T> result = std::make_shared<T>(newId, ctorArgs...);

    {
      std::unique_lock lock(myMapMutex);
      myItems[newId] = result;
    }
    if (myIsTraceEnabled) Log::trace("[%s] created %d", myTemplateName.c_str(), newId);
    return result;
  }

  std::shared_ptr<T> find(int id) {
    if (myIsTraceEnabled) Log::trace("[%s] find %d", myTemplateName.c_str(), id);
    std::unique_lock lock(myMapMutex);
    return myItems[id];
  }

  void dispose(int id) {
    if (myIsTraceEnabled) Log::trace("[%s] dispose %d", myTemplateName.c_str(), id);
    std::unique_lock lock(myMapMutex);
    myItems.erase(id);
  }

  const std::string & getTemplateName() const { return myTemplateName; }

 private:
  std::map<int, std::shared_ptr<T>> myItems;
  std::recursive_mutex myMapMutex;
  std::mutex myIdMutex;
  bool myIsTraceEnabled = false; // only for debugging
  const std::string myTemplateName; // only for debugging
};

template <class T, class D>
class RemoteServerObjectHolder;

// Represents the native server object that owned by corr. factory and can be found by unique id.
template <class T>
class RemoteServerObjectBase {
 public:
  explicit RemoteServerObjectBase(int id) : myId(id) {}
  virtual ~RemoteServerObjectBase() {}

  int getId() { return myId; }

  virtual thrift_codegen::RObject toRObject() {
    thrift_codegen::RObject robj;
    robj.__set_uid(myId);
    robj.isNull = false;
    return robj;
  }

  static std::shared_ptr<T> find(int id) {
    return FACTORY.find(id);
  }

  static std::shared_ptr<T> find(thrift_codegen::RObject robj) {
    return robj.isNull ? nullptr : find(robj.uid);
  }

  static std::shared_ptr<T> get(int id) {
    // The same as find but used when expected not null obj (with logging)
    std::shared_ptr<T> result = FACTORY.find(id);
    if (result == nullptr)
      Log::error("Can't find remote object of type '%s' by id %d", FACTORY.getTemplateName().c_str(), id);
    return result;
  }

  static std::shared_ptr<T> get(thrift_codegen::RObject robj) {
    return robj.isNull ? nullptr : get(robj.uid);
  }

  template<typename... Args>
  static std::shared_ptr<T> create(Args... ctorArgs) { return FACTORY.create(ctorArgs...); }

  static void dispose(int id) { FACTORY.dispose(id); }

 protected:
  const int myId;

  static ServerObjectsFactory<T> FACTORY;
};

// Represents the remote object with some cef delegate.
template <class T, class D>
class RemoteServerObject : public RemoteServerObjectBase<T> {
 public:
  typedef RemoteServerObjectHolder<T, D> Holder;

  explicit RemoteServerObject(int id, CefRefPtr<D> delegate) : RemoteServerObjectBase<T>(id), myDelegate(delegate) {}

  RemoteServerObject(const RemoteServerObject&) = delete;
  RemoteServerObject(RemoteServerObject&&) = delete;

  CefRefPtr<D> getDelegate() { return myDelegate; }

  static std::shared_ptr<T> wrapDelegate(CefRefPtr<D> delegate) {
    if (!delegate) {
      Log::error("wrapDelegate: null delegate");
      return nullptr;
    }
    return RemoteServerObjectBase<T>::create(delegate);
  }

 protected:
  CefRefPtr<D> myDelegate;
};

template <class T, class D>
class RemoteServerObjectWithCache : public RemoteServerObject<T, D> {
 public:
  explicit RemoteServerObjectWithCache(int id, CefRefPtr<D> delegate) : RemoteServerObject<T, D>(id, delegate) {}
  ~RemoteServerObjectWithCache() override {}

  virtual thrift_codegen::RObject toRObject() override {
    thrift_codegen::RObject robj;
    robj.__set_uid(RemoteServerObject<T, D>::myId);
    robj.__set_info(toMap());
    robj.isNull = false;
    return robj;
  }

  void update(const std::map<std::string, std::string>& info) {
    Lock lock(myMutex);
    updateImpl(info);
  }
  std::map<std::string, std::string> toMap() {
    Lock lock(myMutex);
    return toMapImpl();
  }

 protected:
  // Cache support
  std::recursive_mutex myMutex;
  virtual void updateImpl(const std::map<std::string, std::string>&) {}
  virtual std::map<std::string, std::string> toMapImpl() { return std::map<std::string, std::string>(); }
};

// Represents the peer for java object.
// Methods of a java object can be invoked via rpc invocations (via myCtx) with myPeerId.
template <class T>
class RemoteJavaObject {
 public:
  explicit RemoteJavaObject(std::shared_ptr<ServerHandlerContext> ctx, int peerId, std::function<void(JavaService)> disposer)
      : myCtx(ctx),
        myPeerId(peerId),
        myDisposer(std::make_shared<std::function<void(JavaService)>>(disposer)) {}

  explicit RemoteJavaObject(std::shared_ptr<ServerHandlerContext> ctx, int peerId)
      : myCtx(ctx),
        myPeerId(peerId),
        myDisposer(nullptr) {}

  virtual ~RemoteJavaObject() {
    if (myDisposer != nullptr) {
      std::shared_ptr<std::function<void(JavaService)>> d = myDisposer;
      myCtx->invokeLater([=](JavaService s) { d->operator()(s); });
    }
  }

  thrift_codegen::RObject javaId() {
    thrift_codegen::RObject robj;
    robj.__set_uid(myPeerId);
    robj.isNull = false;
    return robj;
  }

 protected:
  const int myPeerId; // java-peer (delegate)
  std::recursive_mutex myMutex;
  std::shared_ptr<ServerHandlerContext> myCtx;
  std::shared_ptr<std::function<void(JavaService)>> myDisposer;
};

template <class T, class D>
class RemoteServerObjectHolder {
 public:
  explicit RemoteServerObjectHolder(CefRefPtr<D>& delegate) {
    if (delegate)
      myRemoteObj = RemoteServerObjectBase<T>::create(delegate);
  }
  ~RemoteServerObjectHolder() {
    if (myRemoteObj != nullptr)
      RemoteServerObject<T, D>::dispose(myRemoteObj->getId());
  }

  thrift_codegen::RObject toRObject() {
    return myRemoteObj != nullptr ? myRemoteObj->toRObject() : thrift_codegen::RObject();
  }

 private:
  std::shared_ptr<T> myRemoteObj = nullptr;
};

template <typename T>
ServerObjectsFactory<T> RemoteServerObjectBase<T>::FACTORY;

#define SET_STR(map, key)                          \
  if (map.count(#key) > 0)                         \
    myDelegate->Set##key(map.at(#key))

#define SET_INT(map, key)                          \
  if (map.count(#key) > 0)                         \
    myDelegate->Set##key(std::stoi(map.at(#key)))

#define SET_LONG(map, key)                          \
  if (map.count(#key) > 0)                         \
    myDelegate->Set##key(std::stoll(map.at(#key)))

#define GET_STR(map, key)                          \
  map[#key] = myDelegate->Get##key().ToString()

#define GET_INT(map, key)                          \
  map[#key] = std::to_string(myDelegate->Get##key())

#define GET_LONG(map, key)                          \
  map[#key] = std::to_string((long long)(myDelegate->Get##key()))

#endif  // JCEF_REMOTEOBJECTS_H
