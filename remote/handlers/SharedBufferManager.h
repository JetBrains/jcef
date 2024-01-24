#ifndef JCEF_SHAREDBUFFERMANAGER_H
#define JCEF_SHAREDBUFFERMANAGER_H

#ifdef WIN32
#include <boost/interprocess/managed_windows_shared_memory.hpp>
#else
#include <boost/interprocess/managed_shared_memory.hpp>
#endif

#include <boost/interprocess/sync/named_mutex.hpp>

class SharedBuffer {
 public:
  SharedBuffer(std::string uid, size_t len);
  ~SharedBuffer();

  void lock();
  void unlock();
  bool tryLock();

  void* ptr() { return mySharedMem; }
  const std::string& uid() { return myUid; }
  int64_t handle() { return mySharedMemHandle; }
  size_t size() { return myLen; }

 private:
  const std::string myUid;
  const size_t myLen;

#ifdef WIN32
  boost::interprocess::managed_windows_shared_memory * mySharedSegment = nullptr;
  boost::interprocess::managed_windows_shared_memory::handle_t mySharedMemHandle{};
#else
  boost::interprocess::managed_shared_memory * mySharedSegment = nullptr;
  boost::interprocess::managed_shared_memory::handle_t mySharedMemHandle{};
#endif
  void * mySharedMem = nullptr;

  boost::interprocess::named_mutex * myMutex;
  void _releaseShared();
};

class SharedBufferManager {
 public:
  SharedBufferManager(int bid);
  ~SharedBufferManager();

  SharedBuffer & getLockedBuffer(size_t size);

 private:
  static constexpr int POOL_SIZE = 2;
  std::string myPrefix;
  SharedBuffer * myPool[POOL_SIZE] = {nullptr, nullptr};
  int myLastUsed = 1;

  SharedBuffer* _getOrCreateBuffer(size_t size, int index);
};

#endif  // JCEF_SHAREDBUFFERMANAGER_H
