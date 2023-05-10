#ifndef JCEF_REMOTEOBJECTFACTORY_H
#define JCEF_REMOTEOBJECTFACTORY_H
#include <mutex>
#include <vector>
#include "../Utils.h"
#include "../log/Log.h"
#include "include/internal/cef_ptr.h"

template <class T>
class RemoteObjectFactory {
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

class RemoteClientHandler;

template <class T>
class RemoteServerObjectBase {
 public:
  explicit RemoteServerObjectBase(RemoteClientHandler & owner, int id) : myId(id), myOwner(owner) {}
  virtual ~RemoteServerObjectBase() { FACTORY.dispose(myId, false); }

  int getId() { return myId; }

  thrift_codegen::RObject toThrift() {
    thrift_codegen::RObject robj;
    robj.__set_objId(myId);
    return robj;
  }

  thrift_codegen::RObject toThriftWithMap() {
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
  RemoteClientHandler & myOwner;

  // Cache support
  virtual void updateImpl(const std::map<std::string, std::string>&) {}
  virtual std::map<std::string, std::string> toMapImpl() { return std::map<std::string, std::string>(); }

  static RemoteObjectFactory<T> FACTORY;
};

template <class T, class D>
class RemoteServerObject : public RemoteServerObjectBase<T> {
 public:
  explicit RemoteServerObject(RemoteClientHandler& owner, int id, CefRefPtr<D> delegate) : RemoteServerObjectBase<T>(owner, id), myDelegate(delegate) {}

  CefRefPtr<D> getDelegate() { return myDelegate; }

 protected:
  CefRefPtr<D> myDelegate;
};

template <class T>
class RemoteObject : public RemoteServerObjectBase<T> {
  typedef RemoteServerObjectBase<T> base;
 public:
  explicit RemoteObject(RemoteClientHandler& owner,
                        int id,
                        int peerId,
                        std::function<void(RpcExecutor::Service)> disposer)
      : RemoteServerObjectBase<T>(owner, id),
        myPeerId(peerId),
        myDisposer(disposer) {}

  virtual ~RemoteObject() {
    base::myOwner.exec([&](RpcExecutor::Service s){
      myDisposer(s);
    });
    base::FACTORY.dispose(base::myId, false);
  }

 protected:
  const int myPeerId; // java-peer (delegate)
  std::function<void(RpcExecutor::Service)> myDisposer;
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
RemoteObjectFactory<T> RemoteServerObjectBase<T>::FACTORY;

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

#endif  // JCEF_REMOTEOBJECTFACTORY_H
