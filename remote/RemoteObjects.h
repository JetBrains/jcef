#ifndef JCEF_REMOTEOBJECTS_H
#define JCEF_REMOTEOBJECTS_H
#include <mutex>
#include <vector>
#include "Utils.h"
#include "handlers/RemoteClientHandler.h"
#include "include/internal/cef_ptr.h"
#include "log/Log.h"

template <class T>
class ServerObjectsFactory {
 public:
  T* create(std::function<T*(int)> creator) {
    Lock lock(MUTEX);

    int freeIndex = 0;
    while (freeIndex < INSTANCES.size() && INSTANCES[freeIndex] != nullptr)
      ++freeIndex;

    T* result = creator(freeIndex);
    if (freeIndex == INSTANCES.size())
      INSTANCES.push_back(result);
    else
      INSTANCES[freeIndex] = result;

    return result;
  }

  T* find(int id) {
    Lock lock(MUTEX);
    return id >= INSTANCES.size() ? nullptr : INSTANCES[id];
  }

  void dispose(int id, bool doDelete) {
    Lock lock(MUTEX);
    if (id >= INSTANCES.size()) return ; // simple protection
    T* r = INSTANCES[id];
    if (r != nullptr) {
      if (doDelete)
        delete r;
      INSTANCES[id] = nullptr;
    }
  }

 private:
  std::vector<T*> INSTANCES;
  std::recursive_mutex MUTEX;
};

template <class T>
class RemoteServerObjectBase {
 public:
  explicit RemoteServerObjectBase(std::shared_ptr<RpcExecutor> service, int id) : myId(id), myService(service) {}
  virtual ~RemoteServerObjectBase() { FACTORY.dispose(myId, false); }

  int getId() { return myId; }

  thrift_codegen::RObject serverId() {
    thrift_codegen::RObject robj;
    robj.__set_objId(myId);
    return robj;
  }

  thrift_codegen::RObject serverIdWithMap() {
    thrift_codegen::RObject robj;
    robj.__set_objId(myId);
    robj.__set_objInfo(toMap());
    return robj;
  }

  // Cache support
  void update(const std::map<std::string, std::string>& info) {
      Lock lock(myMutex);
      updateImpl(info);
  }
  std::map<std::string, std::string> toMap() {
      Lock lock(myMutex);
      return toMapImpl();
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

  static void dispose(int id) { FACTORY.dispose(id, true); }

 protected:
  const int myId;
  std::recursive_mutex myMutex;
  std::shared_ptr<RpcExecutor> myService;

  // Cache support
  virtual void updateImpl(const std::map<std::string, std::string>&) {}
  virtual std::map<std::string, std::string> toMapImpl() { return std::map<std::string, std::string>(); }

  static ServerObjectsFactory<T> FACTORY;
};

template <class T, class D>
class RemoteServerObject : public RemoteServerObjectBase<T> {
 public:
  explicit RemoteServerObject(std::shared_ptr<RpcExecutor> service, int id, CefRefPtr<D> delegate) : RemoteServerObjectBase<T>(service, id), myDelegate(delegate.get()) {
    myDelegate->AddRef();
  }
  ~RemoteServerObject() override {
    myDelegate->Release();
  }

  D& getDelegate() { return *myDelegate; }

 protected:
  D* myDelegate;
};

template <class T>
class RemoteJavaObjectBase {
 public:
  explicit RemoteJavaObjectBase(std::shared_ptr<RpcExecutor> service, int peerId, std::function<void(RpcExecutor::Service)> disposer)
      : myService(service),
        myPeerId(peerId),
        myDisposer(disposer) {}

  virtual ~RemoteJavaObjectBase() {
    myService->exec([&](RpcExecutor::Service s){
      myDisposer(s);
    });
  }

  thrift_codegen::RObject javaId() {
    thrift_codegen::RObject robj;
    robj.__set_objId(myPeerId);
    return robj;
  }

 protected:
  const int myPeerId; // java-peer (delegate)
  std::recursive_mutex myMutex;
  std::shared_ptr<RpcExecutor> myService;
  std::function<void(RpcExecutor::Service)> myDisposer;
};

template <class T>
class RemoteJavaObject : public RemoteJavaObjectBase<T> {
  typedef RemoteJavaObjectBase<T> base;
 public:
  explicit RemoteJavaObject(RemoteClientHandler& owner, int peerId, std::function<void(RpcExecutor::Service)> disposer)
      : RemoteJavaObjectBase<T>(owner.getService(), peerId, disposer),
        myOwner(owner) {}

 protected:
  RemoteClientHandler & myOwner;
};

template <class T>
class Holder {
 public:
  explicit Holder(RemoteServerObjectBase<T>& obj) : myObj(obj) {}
  ~Holder() { RemoteServerObjectBase<T>::dispose(myObj.getId()); }

 private:
  RemoteServerObjectBase<T>& myObj;
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
