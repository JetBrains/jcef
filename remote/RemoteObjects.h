#ifndef JCEF_REMOTEOBJECTS_H
#define JCEF_REMOTEOBJECTS_H
#include <mutex>
#include <vector>
#include "Utils.h"
#include "RpcExecutor.h"
#include "include/internal/cef_ptr.h"
#include "log/Log.h"
#include "ServerHandlerContext.h"

template <class T>
class ServerObjectsFactory {
 public:
  T* create(std::function<T*(int)> creator) {
    if (!myTracePrefix.empty()) Log::trace("[%s] create", myTracePrefix.c_str());
    Lock lock(MUTEX);

    static int id = 0;
    const int newId = id++;
    T* result = creator(newId);
    INSTANCES[newId] = result;
    if (!myTracePrefix.empty()) Log::trace("[%s] created %d", myTracePrefix.c_str(), newId);
    return result;
  }

  T* find(int id) {
    if (!myTracePrefix.empty()) Log::trace("[%s] find %d", myTracePrefix.c_str(), id);
    Lock lock(MUTEX);
    return INSTANCES[id];
  }

  void dispose(int id, bool doDelete) {
    if (!myTracePrefix.empty()) Log::trace("[%s] dispose %d [del=%d]", myTracePrefix.c_str(), id, (int)doDelete);
    T* r = nullptr;
    {
      Lock lock(MUTEX);
      r = INSTANCES[id];
      if (r != nullptr)
        INSTANCES.erase(id);
    }

    if (r != nullptr && doDelete) {
      if (!myTracePrefix.empty()) Log::trace("[%s] delete %d", myTracePrefix.c_str(), id);
      delete r;
      if (!myTracePrefix.empty()) Log::trace("[%s] disposed %d", myTracePrefix.c_str(), id);
    }
  }

  void trace(const std::string & prefix) { myTracePrefix = prefix; }

 private:
  std::map<int, T*> INSTANCES;
  std::recursive_mutex MUTEX;
  std::string myTracePrefix;
};

template <class T, class D>
class RemoteServerObjectHolder;

template <class T>
class RemoteServerObjectBase {
 public:
  explicit RemoteServerObjectBase(int id) : myId(id) {}
  virtual ~RemoteServerObjectBase() { FACTORY.dispose(myId, false); }

  int getId() { return myId; }

  virtual thrift_codegen::RObject serverId() {
    thrift_codegen::RObject robj;
    robj.__set_objId(myId);
    return robj;
  }

  static T* find(int id) {
    return FACTORY.find(id);
  }

  static T* get(int id) {
    T* result = FACTORY.find(id);
    if (result == nullptr)
      Log::error("Can't find remote object by id %d", id);
    return result;
  }

  static T* create(std::function<T*(int)> creator) { return FACTORY.create(creator); }
  static void dispose(int id) { FACTORY.dispose(id, true); }

 protected:
  const int myId;

  static ServerObjectsFactory<T> FACTORY;
};

template <class T, class D>
class RemoteServerObject : public RemoteServerObjectBase<T> {
 public:
  typedef RemoteServerObjectHolder<T, D> Holder;

  explicit RemoteServerObject(int id, CefRefPtr<D> delegate) : RemoteServerObjectBase<T>(id), myDelegate(delegate.get()) {
    myDelegate->AddRef();
  }

  RemoteServerObject(const RemoteServerObject&) = delete;
  RemoteServerObject(RemoteServerObject&&) = delete;

  ~RemoteServerObject() override {
    myDelegate->Release();
  }

  D& getDelegate() { return *myDelegate; }

  static T* wrapDelegate(CefRefPtr<D> delegate) {
    if (!delegate) {
      Log::error("wrapDelegate: null delegate");
      return nullptr;
    }
    return RemoteServerObjectBase<T>::create([&](int id) -> T* {return new T(delegate, id);});
  }

 protected:
  D* myDelegate;
};

template <class T, class D>
class RemoteServerObjectUpdatable : public RemoteServerObject<T, D> {
 public:
  explicit RemoteServerObjectUpdatable(int id, CefRefPtr<D> delegate) : RemoteServerObject<T, D>(id, delegate) {}
  ~RemoteServerObjectUpdatable() override {}

  virtual thrift_codegen::RObject serverId() override {
    thrift_codegen::RObject robj;
    robj.__set_objId(RemoteServerObject<T, D>::myId);
    robj.__set_objInfo(toMap());
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
    robj.__set_objId(myPeerId);
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
      myRemoteObj = RemoteServerObjectBase<T>::create([&](int id) -> T* {return new T(delegate, id);});
  }
  ~RemoteServerObjectHolder() {
    if (myRemoteObj != nullptr)
      RemoteServerObject<T, D>::dispose(myRemoteObj->getId());
  }

  thrift_codegen::RObject serverId() {
    if (myRemoteObj != nullptr)
      return myRemoteObj->serverId();

    thrift_codegen::RObject result;
    result.__set_objId(-1);
    return result;
  }

  T * get() { return myRemoteObj; }

 private:
  T* myRemoteObj = nullptr;
};

template <typename T>
ServerObjectsFactory<T> RemoteServerObjectBase<T>::FACTORY;

#define SET_STR(map, key)                          \
  if (map.count(#key) > 0)                         \
    myDelegate->Set##key(map.at(#key))

#define SET_INT(map, key)                          \
  if (map.count(#key) > 0)                         \
    myDelegate->Set##key(std::stoi(map.at(#key)))

#define GET_STR(map, key)                          \
  map[#key] = myDelegate->Get##key().ToString()

#define GET_INT(map, key)                          \
  map[#key] = std::to_string(myDelegate->Get##key())

#endif  // JCEF_REMOTEOBJECTS_H
